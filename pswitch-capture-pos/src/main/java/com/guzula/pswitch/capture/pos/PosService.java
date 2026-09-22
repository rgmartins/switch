package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosParserService;
import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import org.springframework.stereotype.Service;

/**
 * Referência: pos.service.ts (guzula-switch).
 * Recebe um payload completo da camada de composição, aciona
 * ComumService -> NucleoService, e via
 * {@link #sendResponse} devolve a resposta ao terminal (PosPackerService).
 *
 * TODO: portar o fluxo completo (ver diagrama em guzula-switch/CLAUDE.md).
 */
@Service
public class PosService implements ChannelResponder, InboundPayloadHandler {

    private final OutboundPayloadSender payloadSender;
    private final PosParserService parser;
    private final PosMapperService mapper;

    public PosService(OutboundPayloadSender payloadSender, PosParserService parser, PosMapperService mapper) {
        this.payloadSender = payloadSender;
        this.parser = parser;
        this.mapper = mapper;
    }

    @Override
    public String channel() {
        return "POS";
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
        System.out.printf("Canonical  terminalId=%s valorCentavos=%d moeda=%s%n",
                canonical.getTerminalId(),
                canonical.getOperation().getAmount(),
                canonical.getOperation().getCurrencyCode());

        payloadSender.send(connectionId, payload);
    }
}
