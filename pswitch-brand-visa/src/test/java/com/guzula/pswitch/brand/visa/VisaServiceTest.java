package com.guzula.pswitch.brand.visa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.guzula.pswitch.brand.visa.packer.VisaPackerService;
import com.guzula.pswitch.brand.visa.parser.VisaParserService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.iso8583.Iso8583Codec;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Requer um Redis acessível em localhost:6379 (ver switch-docker/docker-compose.yml) — a correlação
 * do {@link VisaService} agora mora lá, não em memória do processo.
 *
 * <p>{@code authorize()} publica em {@code visa:pedidos} em vez de mandar direto pro socket, e uma
 * thread própria consome {@code visa:respostas} — aqui simulamos o papel do {@code
 * VisaConnectionBridge} (que na vida real roda no processo de comunicação, separado deste)
 * lendo/escrevendo essas mesmas filas direto via Redis.
 *
 * <p>A transação que chega em {@code nucleoService.handleResponse(...)} não é mais o mesmo objeto
 * que {@code authorize()} recebeu (foi serializada e desserializada via Redis no meio do caminho) —
 * por isso os testes capturam o argumento em vez de reler o campo do objeto original.
 */
class VisaServiceTest {

  private static final String REQUEST_QUEUE_KEY = "visa:pedidos";
  private static final String RESPONSE_QUEUE_KEY = "visa:respostas";
  private static final StringRedisTemplate REDIS = redisTemplate();

  @BeforeEach
  void limpaFilas() {
    REDIS.delete(REQUEST_QUEUE_KEY);
    REDIS.delete(RESPONSE_QUEUE_KEY);
  }

  @Test
  void authorizeSendsRequestAndHandleInboundCorrelatesPopulatesAndNotifiesNucleo() {
    NucleoService nucleoService = mock(NucleoService.class);
    VisaService visaService =
        new VisaService(
            new VisaPackerService(),
            new VisaParserService(),
            nucleoService,
            new TableResponseService(),
            REDIS,
            Duration.ofSeconds(10));
    visaService.start();

    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setNsu("1000");
    canonical.setTerminalId("00891592");
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4111111111111111");
    canonical.setCard(card);
    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setAmount(3600);
    canonical.setOperation(operation);
    CanonicalTransaction.Merchant merchant = new CanonicalTransaction.Merchant();
    merchant.setMerchant("10169548130001");
    canonical.setMerchant(merchant);

    visaService.authorize(canonical);
    String hexRequest = REDIS.opsForList().leftPop(REQUEST_QUEUE_KEY, Duration.ofSeconds(3));
    assertNotNull(hexRequest);

    byte[] response = approvedResponse("00891592", 1000, "AB12C3");
    REDIS.opsForList().rightPush(RESPONSE_QUEUE_KEY, HexFormat.of().formatHex(response));

    ArgumentCaptor<CanonicalTransaction> captor =
        ArgumentCaptor.forClass(CanonicalTransaction.class);
    verify(nucleoService, timeout(3000)).handleResponse(captor.capture());
    CanonicalTransaction handled = captor.getValue();
    assertEquals(canonical.getTerminalId(), handled.getTerminalId());
    assertEquals(canonical.getNsu(), handled.getNsu());
    CanonicalTransaction.Response result = handled.getResponse();
    assertNotNull(result);
    assertEquals("00", result.getResponseCode());
    assertEquals("AB12C3", result.getAuthorizationCode());
  }

  @Test
  void handleInboundIgnoresResponseWithoutAPendingTransaction() throws InterruptedException {
    NucleoService nucleoService = mock(NucleoService.class);
    VisaService visaService =
        new VisaService(
            new VisaPackerService(),
            new VisaParserService(),
            nucleoService,
            new TableResponseService(),
            REDIS,
            Duration.ofSeconds(10));
    visaService.start();

    // Nenhum authorize() foi chamado antes — não há nada pendente pra correlacionar.
    byte[] response = approvedResponse("00000000", 1, "ZZZZZZ");
    REDIS.opsForList().rightPush(RESPONSE_QUEUE_KEY, HexFormat.of().formatHex(response));
    Thread.sleep(500); // dá tempo da thread consumidora processar (e não fazer nada) a resposta

    verify(nucleoService, org.mockito.Mockito.never())
        .handleResponse(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void sweepExpiredAuthorizationsBecomesTimeoutDenialAfterResponseTimeout()
      throws InterruptedException {
    NucleoService nucleoService = mock(NucleoService.class);
    VisaService visaService =
        new VisaService(
            new VisaPackerService(),
            new VisaParserService(),
            nucleoService,
            new TableResponseService(),
            REDIS,
            Duration.ofMillis(20));
    visaService.start();

    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setNsu("1000");
    canonical.setTerminalId("00891592");
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4111111111111111");
    canonical.setCard(card);
    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setAmount(3600);
    canonical.setOperation(operation);
    CanonicalTransaction.Merchant merchant = new CanonicalTransaction.Merchant();
    merchant.setMerchant("10169548130001");
    canonical.setMerchant(merchant);

    visaService.authorize(canonical);
    Thread.sleep(30); // passa do timeout de 20ms configurado acima

    visaService.sweepExpiredAuthorizations();

    ArgumentCaptor<CanonicalTransaction> captor =
        ArgumentCaptor.forClass(CanonicalTransaction.class);
    verify(nucleoService).handleResponse(captor.capture());
    CanonicalTransaction.Response response = captor.getValue().getResponse();
    assertNotNull(response);
    assertEquals("91", response.getResponseCode());
    assertEquals(91, response.getObs().getCode());

    // Resposta tardia da Visa não acha mais nada pendente — não sobrescreve o timeout já aplicado.
    byte[] lateResponse = approvedResponse("00891592", 1000, "LATE01");
    REDIS.opsForList().rightPush(RESPONSE_QUEUE_KEY, HexFormat.of().formatHex(lateResponse));
    Thread.sleep(500);
    verify(nucleoService, org.mockito.Mockito.times(1))
        .handleResponse(org.mockito.ArgumentMatchers.any());
  }

  private byte[] approvedResponse(String terminalId, long nsu, String authorizationCode) {
    byte[] bitmap = new byte[8];
    for (int de : new int[] {3, 4, 11, 38, 39, 41}) {
      setBit(bitmap, de);
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(new byte[22]); // cabeçalho (não usado na correlação)
    out.writeBytes(Iso8583Codec.encodeBCD("0110"));
    out.writeBytes(bitmap);
    out.writeBytes(Iso8583Codec.encodeBCD("000000")); // DE3
    out.writeBytes(Iso8583Codec.encodeBCD(3600L, 6)); // DE4
    out.writeBytes(Iso8583Codec.encodeBCD(nsu, 3)); // DE11
    out.writeBytes(Iso8583Codec.encodeEBCDIC(authorizationCode, 6)); // DE38
    out.writeBytes(Iso8583Codec.encodeEBCDIC("00", 2)); // DE39
    out.writeBytes(Iso8583Codec.encodeEBCDIC(terminalId, 8)); // DE41
    return out.toByteArray();
  }

  private void setBit(byte[] bitmap, int de) {
    int byteIndex = (de - 1) / 8;
    int bitIndex = (de - 1) % 8;
    bitmap[byteIndex] |= (byte) (0x80 >> bitIndex);
  }

  private static StringRedisTemplate redisTemplate() {
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
    connectionFactory.afterPropertiesSet();
    StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
    template.afterPropertiesSet();
    return template;
  }
}
