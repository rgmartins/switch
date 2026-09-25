package com.guzula.pswitch.external.hsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Requer um Redis acessível em localhost:6379 (ver switch-docker/docker-compose.yml). O {@link
 * HsmRequestManager} não fala mais com o socket — publica o pedido em {@code hsm:pedidos} e espera
 * a resposta em {@code hsm:resposta:<header>}. Aqui, uma thread simula o papel do {@code
 * HsmConnectionBridge} (que na vida real roda no processo de comunicação, separado deste).
 */
class HsmServiceTest {

  private static final String SOURCE_KEY = "0123456789ABCDEFFEDCBA9876543210";
  private static final String DESTINATION_KEY = "FEDCBA98765432100123456789ABCDEF";
  private static final String REQUEST_QUEUE_KEY = "hsm:pedidos";
  private static final StringRedisTemplate REDIS = redisTemplate();

  @BeforeEach
  void limpaFilaPendente() {
    REDIS.delete(REQUEST_QUEUE_KEY);
  }

  @Test
  void sendsSeWaitsForSfAndPopulatesCard() throws InterruptedException {
    String track = "4123456789012349=29122010000000000000";
    AtomicReference<byte[]> requestSent = new AtomicReference<>();
    Thread hsmSimulator =
        simulateHsm(
            request -> {
              requestSent.set(request.clone());
              String header = new String(request, 0, 4, StandardCharsets.US_ASCII);
              return (header + "SF00" + "%05d".formatted(track.length()) + track)
                  .getBytes(StandardCharsets.US_ASCII);
            });

    HsmService service =
        new HsmService(
            new HsmRequestManager(REDIS, Duration.ofSeconds(2)),
            new HsmSeProtocol(),
            new HsmG0Protocol());
    CanonicalTransaction canonical = canonical();

    service.decryptCardData(canonical, SOURCE_KEY);
    hsmSimulator.join();

    byte[] request = requestSent.get();
    String requestPrefix = new String(request, 0, 69, StandardCharsets.US_ASCII);
    assertTrue(requestPrefix.startsWith("0000SE01" + SOURCE_KEY + "505FFFFF1700168A060069C3"));
    assertTrue(requestPrefix.endsWith("00006"));
    assertEquals(";99%01", new String(request, request.length - 6, 6, StandardCharsets.US_ASCII));
    assertEquals(track, canonical.getCard().getTrack());
    assertEquals("4123456789012349", canonical.getCard().getCardNumber());
    assertEquals("2912", canonical.getCard().getExpirationDate());
    assertEquals("201", canonical.getCard().getServiceCode());
  }

  @Test
  void failsWhenSfDoesNotArriveBeforeTimeout() {
    // Ninguém simula o HSM aqui — o pedido fica publicado em hsm:pedidos, sem resposta.
    HsmService service =
        new HsmService(
            new HsmRequestManager(REDIS, Duration.ofMillis(20)),
            new HsmSeProtocol(),
            new HsmG0Protocol());

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class, () -> service.decryptCardData(canonical(), SOURCE_KEY));

    assertTrue(error.getMessage().contains("Timeout aguardando resposta SF"));
  }

  @Test
  void sendsG0WaitsForG1AndUpdatesPinBlock() throws InterruptedException {
    String translatedPinBlock = "FEDCBA9876543210";
    AtomicReference<byte[]> requestSent = new AtomicReference<>();
    Thread hsmSimulator =
        simulateHsm(
            request -> {
              requestSent.set(request.clone());
              String header = new String(request, 0, 4, StandardCharsets.US_ASCII);
              return (header + "G10016" + translatedPinBlock).getBytes(StandardCharsets.US_ASCII);
            });

    HsmService service =
        new HsmService(
            new HsmRequestManager(REDIS, Duration.ofSeconds(2)),
            new HsmSeProtocol(),
            new HsmG0Protocol());
    CanonicalTransaction canonical = canonical();

    service.translatePinBlock(canonical, SOURCE_KEY, DESTINATION_KEY);
    hsmSimulator.join();

    String request = new String(requestSent.get(), StandardCharsets.US_ASCII);
    assertEquals(
        "0000G0"
            + SOURCE_KEY
            + DESTINATION_KEY
            + "A05FFFFF1700168A060069C0123456789ABCDEF0101345678901234%01",
        request);
    assertEquals(translatedPinBlock, canonical.getSecurity().getPinBlock());
  }

  /**
   * Simula o {@code HsmConnectionBridge}: numa thread separada, espera um pedido aparecer em {@code
   * hsm:pedidos}, gera a resposta com {@code responder} e publica em {@code hsm:resposta:<header>}
   * — exatamente o papel que, em produção, roda no processo de comunicação, não neste.
   */
  private static Thread simulateHsm(UnaryOperator<byte[]> responder) {
    Thread thread =
        new Thread(
            () -> {
              String hexRequest =
                  REDIS.opsForList().leftPop(REQUEST_QUEUE_KEY, Duration.ofSeconds(5));
              if (hexRequest == null) {
                return;
              }
              byte[] request = HexFormat.of().parseHex(hexRequest);
              byte[] response = responder.apply(request);
              String header = new String(response, 0, 4, StandardCharsets.US_ASCII);
              REDIS
                  .opsForList()
                  .rightPush("hsm:resposta:" + header, HexFormat.of().formatHex(response));
            });
    thread.start();
    return thread;
  }

  private CanonicalTransaction canonical() {
    CanonicalTransaction.Security.Ksn ksn = new CanonicalTransaction.Security.Ksn();
    ksn.setBdkIndicator("FFFFF17001");
    ksn.setPinPadIndicator("68A06");
    ksn.setTransactionCounter("0069C");
    CanonicalTransaction.Security security = new CanonicalTransaction.Security();
    security.setKsn(ksn);
    security.setEncryptedCardData("A1B2C3D4E5F6");
    security.setPinBlock("0123456789ABCDEF");
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setSecurity(security);
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4123456789012349");
    canonical.setCard(card);
    return canonical;
  }

  private static StringRedisTemplate redisTemplate() {
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
    connectionFactory.afterPropertiesSet();
    StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
    template.afterPropertiesSet();
    return template;
  }
}
