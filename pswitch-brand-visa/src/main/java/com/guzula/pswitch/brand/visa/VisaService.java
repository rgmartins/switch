package com.guzula.pswitch.brand.visa;

import com.guzula.pswitch.brand.visa.packer.VisaPackerService;
import com.guzula.pswitch.brand.visa.parser.VisaAuthorizationResponse;
import com.guzula.pswitch.brand.visa.parser.VisaParserService;
import com.guzula.pswitch.nucleo.BrandHandler;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.shared.SwitchConstants;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por
 * terminalId+NSU — em memória (um {@link ConcurrentHashMap}), diferente do original, que usa Redis;
 * suficiente enquanto o switch roda numa instância só.
 *
 * <p>TODO: timeout de correlação (transação nunca respondida pela bandeira fica presa no map pra
 * sempre); portar pra Redis se/quando precisar de múltiplas instâncias.
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
  private final Map<String, CanonicalTransaction> pendingByCorrelationKey =
      new ConcurrentHashMap<>();

  public VisaService(
      VisaPackerService packerService,
      VisaParserService parserService,
      OutboundPayloadSender payloadSender,
      NucleoService nucleoService) {
    this.packerService = packerService;
    this.parserService = parserService;
    this.payloadSender = payloadSender;
    this.nucleoService = nucleoService;
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
    pendingByCorrelationKey.put(
        correlationKey(transaction.getTerminalId(), transaction.getNsu()), transaction);
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
    CanonicalTransaction transaction = pendingByCorrelationKey.remove(key);
    if (transaction == null) {
      LOGGER.warning(
          () ->
              "[VisaService] Resposta sem transação correspondente (chave %s): %s"
                  .formatted(key, bytesToHex(payload)));
      return;
    }

    populateResponse(transaction, response);
    LOGGER.info(
        () ->
            "[VisaService] Resposta da Visa recebida: código=%s, autorização=%s, chave=%s"
                .formatted(response.responseCode(), response.authorizationCode(), key));
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
}
