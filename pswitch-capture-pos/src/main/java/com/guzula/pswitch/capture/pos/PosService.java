package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosParserService;
import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
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

  private static final String POS = "POS";

  private final OutboundPayloadSender payloadSender;
  private final PosParserService parser;
  private final PosMapperService mapper;
  private final ComumService comumService;

  public PosService(
      OutboundPayloadSender payloadSender,
      PosParserService parser,
      PosMapperService mapper,
      ComumService comumService) {
    this.payloadSender = payloadSender;
    this.parser = parser;
    this.mapper = mapper;
    this.comumService = comumService;
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
    PosMessage message = parser.parse(payload);
    System.out.print(message.toMultilineString());

    CanonicalTransaction canonical = mapper.toCanonical(message, connectionId);
    comumService.process(canonical);
    System.out.println("****************************************");
    System.out.println("* Canônico com dados do terminal new   *");
    System.out.println("****************************************");
    System.out.println(canonical);
    System.out.printf(
        "Canonical  terminalId=%s valorCentavos=%d moeda=%s%n",
        canonical.getTerminalId(),
        canonical.getOperation().getAmount(),
        canonical.getOperation().getCurrencyCode());

    payloadSender.send(connectionId, payload);
  }
}
