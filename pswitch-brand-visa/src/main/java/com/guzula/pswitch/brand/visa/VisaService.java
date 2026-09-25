package com.guzula.pswitch.brand.visa;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por
 * terminalId+NSU — em memória (um {@link ConcurrentHashMap}), diferente do original, que usa Redis;
 * suficiente enquanto o switch roda numa instância só.
 *
 * <p>{@link #sweepExpiredAuthorizations} varre periodicamente as transações nunca respondidas
 * dentro de {@code responseTimeout} e as converte em negação — sem isso, uma bandeira que nunca
 * responde deixaria a transação (e o POS) esperando pra sempre. Referência: handleTimeout
 * (visa.service.ts).
 *
 * <p>TODO: portar pra Redis se/quando precisar de múltiplas instâncias do switch.
 */
@Service
public class VisaService implements BrandHandler, InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(VisaService.class.getName());
  private static final String HANDLER_NAME = "VISA";
  private static final String APPROVED_RESPONSE_CODE = "00";

  private final VisaPackerService packerService;
  private final VisaParserService parserService;
  private final OutboundPayloadSender payloadSender;
  private final NucleoService nucleoService;
  private final TableResponseService tableResponseService;
  private final Duration responseTimeout;
  private final Map<String, PendingAuthorization> pendingByCorrelationKey =
      new ConcurrentHashMap<>();

  public VisaService(
      VisaPackerService packerService,
      VisaParserService parserService,
      OutboundPayloadSender payloadSender,
      NucleoService nucleoService,
      TableResponseService tableResponseService,
      @Value("${pswitch.visa.response-timeout:10s}") Duration responseTimeout) {
    this.packerService = packerService;
    this.parserService = parserService;
    this.payloadSender = payloadSender;
    this.nucleoService = nucleoService;
    this.tableResponseService = tableResponseService;
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
    pendingByCorrelationKey.put(key, new PendingAuthorization(transaction, Instant.now()));
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
    PendingAuthorization pending = pendingByCorrelationKey.remove(key);
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
    for (Map.Entry<String, PendingAuthorization> entry : pendingByCorrelationKey.entrySet()) {
      PendingAuthorization pending = entry.getValue();
      if (Duration.between(pending.sentAt(), now).compareTo(responseTimeout) < 0) {
        continue;
      }
      // remove só se ninguém já processou essa mesma entrada nesse meio tempo (handleInbound).
      if (pendingByCorrelationKey.remove(entry.getKey(), pending)) {
        timeout(pending.transaction(), entry.getKey());
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
