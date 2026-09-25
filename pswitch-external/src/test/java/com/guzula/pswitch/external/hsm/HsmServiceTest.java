package com.guzula.pswitch.external.hsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guzula.pswitch.comunicacao.hsm.HsmRequestManager;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Requer um Redis acessível em localhost:6379 (ver switch-docker/docker-compose.yml) — a correlação
 * do {@link HsmRequestManager} agora mora lá, não em memória do processo.
 */
class HsmServiceTest {

  private static final String SOURCE_KEY = "0123456789ABCDEFFEDCBA9876543210";
  private static final String DESTINATION_KEY = "FEDCBA98765432100123456789ABCDEF";
  private static final StringRedisTemplate REDIS = redisTemplate();

  @Test
  void sendsSeWaitsForSfAndPopulatesCard() {
    AtomicReference<byte[]> requestSent = new AtomicReference<>();
    AtomicReference<HsmService> serviceReference = new AtomicReference<>();
    String track = "4123456789012349=29122010000000000000";
    HsmService service =
        new HsmService(
            new HsmRequestManager(
                (connectionId, payload) -> {
                  assertEquals("HSM", connectionId);
                  requestSent.set(payload.clone());
                  String header = new String(payload, 0, 4, StandardCharsets.US_ASCII);
                  byte[] response =
                      (header + "SF00" + "%05d".formatted(track.length()) + track)
                          .getBytes(StandardCharsets.US_ASCII);
                  serviceReference.get().handleInbound(connectionId, response);
                },
                REDIS,
                Duration.ofSeconds(1)),
            new HsmSeProtocol(),
            new HsmG0Protocol());
    serviceReference.set(service);
    CanonicalTransaction canonical = canonical();

    service.decryptCardData(canonical, SOURCE_KEY);

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
    HsmService service =
        new HsmService(
            new HsmRequestManager((connectionId, payload) -> {}, REDIS, Duration.ofMillis(20)),
            new HsmSeProtocol(),
            new HsmG0Protocol());

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class, () -> service.decryptCardData(canonical(), SOURCE_KEY));

    assertTrue(error.getMessage().contains("Timeout aguardando resposta SF"));
  }

  @Test
  void sendsG0WaitsForG1AndUpdatesPinBlock() {
    AtomicReference<byte[]> requestSent = new AtomicReference<>();
    AtomicReference<HsmService> serviceReference = new AtomicReference<>();
    String translatedPinBlock = "FEDCBA9876543210";
    HsmService service =
        new HsmService(
            new HsmRequestManager(
                (connectionId, payload) -> {
                  requestSent.set(payload.clone());
                  String header = new String(payload, 0, 4, StandardCharsets.US_ASCII);
                  byte[] response =
                      (header + "G10016" + translatedPinBlock).getBytes(StandardCharsets.US_ASCII);
                  serviceReference.get().handleInbound(connectionId, response);
                },
                REDIS,
                Duration.ofSeconds(1)),
            new HsmSeProtocol(),
            new HsmG0Protocol());
    serviceReference.set(service);
    CanonicalTransaction canonical = canonical();

    service.translatePinBlock(canonical, SOURCE_KEY, DESTINATION_KEY);

    String request = new String(requestSent.get(), StandardCharsets.US_ASCII);
    assertEquals(
        "0000G0"
            + SOURCE_KEY
            + DESTINATION_KEY
            + "A05FFFFF1700168A060069C0123456789ABCDEF0101345678901234%01",
        request);
    assertEquals(translatedPinBlock, canonical.getSecurity().getPinBlock());
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
