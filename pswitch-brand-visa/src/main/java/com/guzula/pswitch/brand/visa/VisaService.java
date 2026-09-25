package com.guzula.pswitch.brand.visa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guzula.pswitch.brand.visa.packer.VisaPackerService;
import com.guzula.pswitch.brand.visa.parser.VisaAuthorizationResponse;
import com.guzula.pswitch.brand.visa.parser.VisaParserService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.nucleo.BrandHandler;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.shared.SwitchConstants;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import com.guzula.pswitch.shared.rules.Obs;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por
 * terminalId+NSU — via Redis (chave {@code visa:pendente:<chave>}), igual ao original, para que a
 * resposta possa ser processada por uma réplica diferente da que enviou o pedido. Ver
 * docs/arquitetura/topologia-implantacao.md.
 *
 * <p>{@link #sweepExpiredAuthorizations} varre periodicamente as transações nunca respondidas
 * dentro de {@code responseTimeout} e as converte em negação — sem isso, uma bandeira que nunca
 * responde deixaria a transação (e o POS) esperando pra sempre. Referência: handleTimeout
 * (visa.service.ts).
 */
@Service
public class VisaService implements BrandHandler, InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(VisaService.class.getName());
  private static final String HANDLER_NAME = "VISA";
  private static final String APPROVED_RESPONSE_CODE = "00";
  private static final String PENDING_KEY_PREFIX = "visa:pendente:";
  private static final Duration PENDING_TTL_BUFFER = Duration.ofSeconds(30);

  private final VisaPackerService packerService;
  private final VisaParserService parserService;
  private final OutboundPayloadSender payloadSender;
  private final NucleoService nucleoService;
  private final TableResponseService tableResponseService;
  private final StringRedisTemplate redisTemplate;
  private final Duration responseTimeout;
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public VisaService(
      VisaPackerService packerService,
      VisaParserService parserService,
      OutboundPayloadSender payloadSender,
      NucleoService nucleoService,
      TableResponseService tableResponseService,
      StringRedisTemplate redisTemplate,
      @Value("${pswitch.visa.response-timeout:10s}") Duration responseTimeout) {
    this.packerService = packerService;
    this.parserService = parserService;
    this.payloadSender = payloadSender;
    this.nucleoService = nucleoService;
    this.tableResponseService = tableResponseService;
    this.redisTemplate = redisTemplate;
    this.responseTimeout = responseTimeout;
  }

  @Override
  public String brand() {
    return String.valueOf(SwitchConstants.Brand.VISA);
  }

  @Override
  public String handlerName() {
    return HANDLER_NAME;
  }

  @Override
  public void authorize(CanonicalTransaction transaction) {
    String key = correlationKey(transaction.getTerminalId(), transaction.getNsu());
    storePending(key, new PendingAuthorization(transaction, Instant.now()));
    byte[] request = packerService.pack(transaction);
    payloadSender.send(HANDLER_NAME, request);
    LOGGER.info(
        () ->
            "[VisaService] Mensagem enviada ao simulador Visa (%d bytes): %s"
                .formatted(request.length, bytesToHex(request)));
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    VisaAuthorizationResponse response = parserService.parse(payload);
    String key = correlationKey(response.terminalId(), String.valueOf(response.nsu()));
    PendingAuthorization pending = claimPending(key);
    if (pending == null) {
      LOGGER.warning(
          () ->
              "[VisaService] Resposta sem transação correspondente (chave %s): %s"
                  .formatted(key, bytesToHex(payload)));
      return;
    }

    populateResponse(pending.transaction(), response);
    LOGGER.info(
        () ->
            "[VisaService] Resposta da Visa recebida: código=%s, autorização=%s, chave=%s"
                .formatted(response.responseCode(), response.authorizationCode(), key));
    nucleoService.handleResponse(pending.transaction());
  }

  /**
   * Converte em negação (timeout) qualquer autorização parada há mais de {@code responseTimeout}.
   */
  @Scheduled(fixedDelayString = "${pswitch.visa.timeout-sweep-interval:1s}")
  void sweepExpiredAuthorizations() {
    Instant now = Instant.now();
    ScanOptions scanOptions =
        ScanOptions.scanOptions().match(PENDING_KEY_PREFIX + "*").count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
      while (cursor.hasNext()) {
        String redisKey = cursor.next();
        PendingAuthorization candidate = peekPending(redisKey);
        if (candidate == null
            || Duration.between(candidate.sentAt(), now).compareTo(responseTimeout) < 0) {
          continue;
        }
        // reivindica de forma atômica só agora — se handleInbound já pegou essa mesma chave nesse
        // meio tempo, claimPendingByRedisKey devolve null e não fazemos nada.
        PendingAuthorization claimed = claimPendingByRedisKey(redisKey);
        if (claimed != null) {
          timeout(claimed.transaction(), redisKey.substring(PENDING_KEY_PREFIX.length()));
        }
      }
    }
  }

  private void timeout(CanonicalTransaction transaction, String key) {
    tableResponseService.populateResponseFromRule(transaction, Obs.RULE_091_BRAND_TIMEOUT);
    LOGGER.warning(
        () -> "[VisaService] Timeout aguardando resposta da Visa (chave %s)".formatted(key));
    nucleoService.handleResponse(transaction);
  }

  private void populateResponse(
      CanonicalTransaction transaction, VisaAuthorizationResponse visaResponse) {
    CanonicalTransaction.Response response = new CanonicalTransaction.Response();
    response.setWhoResponded(SwitchConstants.WhoResponded.BRAND_VISA);
    response.setResponseCode(visaResponse.responseCode());
    response.setIssuerResponseCode(visaResponse.responseCode());
    response.setAuthorizationCode(visaResponse.authorizationCode());
    response.setMessage(
        APPROVED_RESPONSE_CODE.equals(visaResponse.responseCode()) ? "APROVADA" : "NAO AUTORIZADA");
    response.setMessageSource(SwitchConstants.MessageDescriptionSource.BRAND);
    response.setPaymentAccountReference("");
    CanonicalTransaction.Emv emv = new CanonicalTransaction.Emv();
    response.setEmv(emv);
    transaction.setResponse(response);
  }

  private void storePending(String correlationKey, PendingAuthorization pending) {
    redisTemplate
        .opsForValue()
        .set(
            PENDING_KEY_PREFIX + correlationKey,
            writeJson(pending),
            responseTimeout.plus(PENDING_TTL_BUFFER));
  }

  private PendingAuthorization peekPending(String redisKey) {
    String json = redisTemplate.opsForValue().get(redisKey);
    return json == null ? null : readJson(json);
  }

  private PendingAuthorization claimPending(String correlationKey) {
    return claimPendingByRedisKey(PENDING_KEY_PREFIX + correlationKey);
  }

  private PendingAuthorization claimPendingByRedisKey(String redisKey) {
    String json = redisTemplate.opsForValue().getAndDelete(redisKey);
    return json == null ? null : readJson(json);
  }

  private String writeJson(PendingAuthorization pending) {
    try {
      return objectMapper.writeValueAsString(pending);
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao serializar autorização pendente", exception);
    }
  }

  private PendingAuthorization readJson(String json) {
    try {
      return objectMapper.readValue(json, PendingAuthorization.class);
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao desserializar autorização pendente", exception);
    }
  }

  private static String correlationKey(String terminalId, String nsu) {
    return terminalId + nsu;
  }

  private static String bytesToHex(byte[] payload) {
    StringBuilder hex = new StringBuilder(payload.length * 2);
    for (byte b : payload) {
      hex.append(String.format("%02X", b));
    }
    return hex.toString();
  }

  private record PendingAuthorization(CanonicalTransaction transaction, Instant sentAt) {}
}
