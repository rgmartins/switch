package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosParserService;
import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Referência: pos.service.ts (guzula-switch). Recebe um payload completo da camada de composição,
 * aciona ComumService -> NucleoService, e via {@link #sendResponse} devolve a resposta ao terminal
 * (PosPackerService).
 *
 * <p>TODO: portar o fluxo completo (ver diagrama em guzula-switch/CLAUDE.md). Por enquanto {@link
 * #sendResponse} apenas ecoa de volta o payload bruto recebido, até {@link PosPackerService}
 * empacotar a resposta real a partir de {@code canonical.response}.
 */
@Service
public class PosService implements ChannelResponder, InboundPayloadHandler {

  private static final String POS = "POS";

  private final OutboundPayloadSender payloadSender;
  private final PosParserService parser;
  private final PosMapperService mapper;
  private final NucleoService nucleoService;
  private final Map<String, byte[]> pendingRequestByConnection = new ConcurrentHashMap<>();

  public PosService(
      OutboundPayloadSender payloadSender,
      PosParserService parser,
      PosMapperService mapper,
      NucleoService nucleoService) {
    this.payloadSender = payloadSender;
    this.parser = parser;
    this.mapper = mapper;
    this.nucleoService = nucleoService;
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
    String connectionId = transaction.getCommunication().getSocketId();
    byte[] payload = pendingRequestByConnection.remove(connectionId);
    logCanonical(transaction);
    // TODO: portar pos.service.ts#sendResponseToPOS — empacotar transaction.response via
    // PosPackerService em vez de ecoar o payload bruto recebido.
    payloadSender.send(connectionId, payload);
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    PosMessage message = parser.parse(payload);
    System.out.print(message.toMultilineString());

    CanonicalTransaction canonical = mapper.toCanonical(message, connectionId);
    pendingRequestByConnection.put(connectionId, payload);
    // processTransaction chama handleResponse -> sendResponse (deste próprio PosService) no final
    // — não devolvemos a resposta aqui, para manter o roteamento por canal centralizado no
    // NucleoService.
    nucleoService.processTransaction(canonical);
  }

  private void logCanonical(CanonicalTransaction transaction) {
    System.out.println("****************************************");
    System.out.println("* Canônico com dados do terminal new   *");
    System.out.println("****************************************");
    System.out.println(transaction);
    System.out.printf(
        "Canonical  terminalId=%s valorCentavos=%d moeda=%s%n",
        transaction.getTerminalId(),
        transaction.getOperation().getAmount(),
        transaction.getOperation().getCurrencyCode());
  }
}
