package com.guzula.pswitch.brand.visa;

import com.guzula.pswitch.brand.visa.packer.VisaPackerService;
import com.guzula.pswitch.nucleo.BrandHandler;
import com.guzula.pswitch.shared.SwitchConstants;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por RRN (via
 * Redis, no original).
 *
 * <p>TODO: portar correlação de resposta (Redis) e timeout. {@link #handleInbound} hoje só loga o
 * que volta do simulador — falta ligar de volta em NucleoService.handleResponse.
 */
@Service
public class VisaService implements BrandHandler, InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(VisaService.class.getName());
  private static final String HANDLER_NAME = "VISA";

  private final VisaPackerService packerService;
  private final OutboundPayloadSender payloadSender;

  public VisaService(VisaPackerService packerService, OutboundPayloadSender payloadSender) {
    this.packerService = packerService;
    this.payloadSender = payloadSender;
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
  public void handleInbound(String connectionId, byte[] payload) {
    LOGGER.info(
        () ->
            "[VisaService] Recebido do simulador Visa (%s): %d bytes: %s"
                .formatted(connectionId, payload.length, bytesToHex(payload)));
  }

  @Override
  public void authorize(CanonicalTransaction transaction) {
    byte[] request = packerService.pack(transaction);
    payloadSender.send(HANDLER_NAME, request);
    LOGGER.info(
        () ->
            "[VisaService] Mensagem enviada ao simulador Visa (%d bytes): %s"
                .formatted(request.length, bytesToHex(request)));
  }

  private static String bytesToHex(byte[] payload) {
    StringBuilder hex = new StringBuilder(payload.length * 2);
    for (byte b : payload) {
      hex.append(String.format("%02X", b));
    }
    return hex.toString();
  }
}
