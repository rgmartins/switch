package com.guzula.pswitch.brand.visa;

import com.guzula.pswitch.nucleo.BrandHandler;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por RRN (via
 * Redis, no original).
 *
 * <p>TODO: portar envio outbound (pswitch-transport), VisaParserService/VisaPackerService e
 * correlação de resposta. {@link #handleInbound} hoje só loga — serve pra validar a conexão TCP
 * outbound com o simulador antes do protocolo estar pronto.
 */
@Service
public class VisaService implements BrandHandler, InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(VisaService.class.getName());
  private static final String CHANNEL = "VISA";

  @Override
  public String brand() {
    return CHANNEL;
  }

  @Override
  public String handlerName() {
    return CHANNEL;
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
    throw new UnsupportedOperationException("TODO: portar visa.service.ts");
  }

  private static String bytesToHex(byte[] payload) {
    StringBuilder hex = new StringBuilder(payload.length * 2);
    for (byte b : payload) {
      hex.append(String.format("%02X", b));
    }
    return hex.toString();
  }
}
