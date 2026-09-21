package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

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

    @Override
    public String channel() {
        return "POS";
    }

    @Override
    public void sendResponse(CanonicalTransaction transaction) {
        throw new UnsupportedOperationException("TODO: portar pos.service.ts");
    }

    @Override
    public void handleInbound(byte[] payload, Consumer<byte[]> responder) {
        String message = new String(payload, StandardCharsets.UTF_8);
        System.out.println("POS recebeu: " + message);

        String response = "recebi: " + message + ", e estou dizendo que foi ok";
        responder.accept(response.getBytes(StandardCharsets.UTF_8));
    }
}
