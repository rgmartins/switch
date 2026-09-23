package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosParserService;
import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * Referência: pos.service.ts (guzula-switch). Recebe um payload completo da camada de composição,
 * aciona ComumService -> NucleoService, e via {@link #sendResponse} devolve a resposta ao terminal
 * (PosPackerService).
 *
 * <p>TODO: portar o fluxo completo (ver diagrama em guzula-switch/CLAUDE.md).
 */
@Service
public class PosService implements ChannelResponder, InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(PosService.class.getName());
  private static final String POS = "POS";
  private static final String HSM_CONNECTION_ID = "HSM";
  private static final String G0_HEADER = "9876";
  private static final String MOCK_PIN_BLOCK = "0123456789ABCDEF";

  private final OutboundPayloadSender payloadSender;
  private final PosParserService parser;
  private final PosMapperService mapper;
  private final AtomicReference<byte[]> lastG1Response = new AtomicReference<>();

  public PosService(
      OutboundPayloadSender payloadSender, PosParserService parser, PosMapperService mapper) {
    this.payloadSender = payloadSender;
    this.parser = parser;
    this.mapper = mapper;
  }

  @Override
  public String handlerName() {
    return POS;
  }

  @Override
  public String channelName() {
    return POS;
  }

  @Override
  public void sendResponse(CanonicalTransaction transaction) {
    throw new UnsupportedOperationException("TODO: portar pos.service.ts");
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    if (HSM_CONNECTION_ID.equalsIgnoreCase(connectionId)) {
      lastG1Response.set(payload.clone());
      receiveComandoG0();
      return;
    }

    PosMessage message = parser.parse(payload);
    System.out.print(message.toMultilineString());

    CanonicalTransaction canonical = mapper.toCanonical(message, connectionId);
    System.out.printf(
        "Canonical  terminalId=%s valorCentavos=%d moeda=%s%n",
        canonical.getTerminalId(),
        canonical.getOperation().getAmount(),
        canonical.getOperation().getCurrencyCode());

    payloadSender.send(connectionId, payload);
    sendComandoG0();
  }

  public void sendComandoG0() {
    byte[] command =
        (G0_HEADER
                + "G0"
                + "S".repeat(32)
                + "D".repeat(32)
                + "A05"
                + "12345678901234567890"
                + MOCK_PIN_BLOCK
                + "0101"
                + "123456789012"
                + "%01")
            .getBytes(StandardCharsets.US_ASCII);

    payloadSender.send(HSM_CONNECTION_ID, command);
    LOGGER.info(() -> "Comando G0 mock enviado ao HSM (" + command.length + " bytes)");
  }

  public void receiveComandoG0() {
    byte[] payload = lastG1Response.getAndSet(null);
    if (payload == null) {
      throw new IllegalStateException("Nenhuma resposta G1 está disponível");
    }

    String response = new String(payload, StandardCharsets.US_ASCII);
    String expectedPrefix = G0_HEADER + "G10016";
    if (response.length() != expectedPrefix.length() + 16 || !response.startsWith(expectedPrefix)) {
      throw new IllegalArgumentException("Resposta G1 inválida: " + response);
    }

    LOGGER.info("Resposta G1 recebida e validada com sucesso");
  }
}
