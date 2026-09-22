package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import org.springframework.stereotype.Service;

import java.util.HexFormat;

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

    public PosService(OutboundPayloadSender payloadSender) {
        this.payloadSender = payloadSender;
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
        System.out.printf(
                "POS recebeu (%d bytes): %s%n",
                payload.length,
                HexFormat.of().formatHex(payload));

        payloadSender.send(connectionId, payload);
    }
}
