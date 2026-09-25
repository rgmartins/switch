package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Coordena os comandos enviados ao HSM.
 *
 * <p>Não fala com o socket, nem sabe que existe — manda o comando e espera a resposta através do
 * {@link HsmRequestManager} (fila), que por sua vez é atendida pelo {@code HsmConnectionBridge} no
 * processo separado de comunicação. Por isso não implementa mais {@code InboundPayloadHandler}: não
 * há conexão nenhuma aqui pra registrar.
 */
@Service
public class HsmService {

  private static final Logger LOGGER = Logger.getLogger(HsmService.class.getName());

  private final HsmRequestManager requestManager;
  private final HsmSeProtocol seProtocol;
  private final HsmG0Protocol g0Protocol;

  public HsmService(
      HsmRequestManager requestManager, HsmSeProtocol seProtocol, HsmG0Protocol g0Protocol) {
    this.requestManager = requestManager;
    this.seProtocol = seProtocol;
    this.g0Protocol = g0Protocol;
  }

  public void decryptCardData(CanonicalTransaction canonical, String sourceKey) {
    byte[] request = seProtocol.buildSeRequest(canonical, sourceKey);
    byte[] response = requestManager.sendAndWait("SE", "SF", request);

    HsmSeProtocol.SeResult result = seProtocol.parseSfResponse(response);
    populateCard(canonical, result);
    LOGGER.info("Resposta SF processada com sucesso");
  }

  public void translatePinBlock(
      CanonicalTransaction canonical, String sourceKey, String destinationKey) {
    byte[] request = g0Protocol.buildG0Request(canonical, sourceKey, destinationKey);
    byte[] response = requestManager.sendAndWait("G0", "G1", request);

    HsmG0Protocol.G1Result result = g0Protocol.parseG1Response(response);
    populateTranslatedPinBlock(canonical, result);
    LOGGER.info("Resposta G1 processada com sucesso");
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

  private void populateTranslatedPinBlock(
      CanonicalTransaction canonical, HsmG0Protocol.G1Result result) {
    canonical.getSecurity().setPinBlock(result.pinBlock());
  }
}
