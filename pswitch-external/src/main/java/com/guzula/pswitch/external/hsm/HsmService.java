package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/** Coordena os comandos enviados ao HSM. */
@Service
public class HsmService implements InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(HsmService.class.getName());
  static final String CONNECTION_NAME = "HSM";

  private final HsmRequestManager requestManager;
  private final HsmSeProtocol seProtocol;

  public HsmService(HsmRequestManager requestManager, HsmSeProtocol seProtocol) {
    this.requestManager = requestManager;
    this.seProtocol = seProtocol;
  }

  @Override
  public String handlerName() {
    return CONNECTION_NAME;
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    requestManager.complete(payload);
  }

  public void decryptCardData(CanonicalTransaction canonical, String sourceKey) {
    byte[] request = seProtocol.buildSeRequest(canonical, sourceKey);
    byte[] response = requestManager.sendAndWait("SE", "SF", request);

    HsmSeProtocol.SeResult result = seProtocol.parseSfResponse(response);
    populateCard(canonical, result);
    LOGGER.info("Resposta SF processada com sucesso");
  }

  private void populateCard(CanonicalTransaction canonical, HsmSeProtocol.SeResult result) {
    CanonicalTransaction.Card card = canonical.getCard();
    if (card == null) {
      card = new CanonicalTransaction.Card();
      canonical.setCard(card);
    }

    card.setTrack(result.track());
    card.setCardNumber(result.cardNumber());
    card.setExpirationDate(result.expirationDate());
    card.setServiceCode(result.serviceCode());
  }
}
