package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/**
 * Referência: pos.service.ts (guzula-switch).
 * Recebe do TcpRawServer, aciona ComumService -> NucleoService, e via
 * {@link #sendResponse} devolve a resposta ao terminal (PosPackerService).
 *
 * TODO: portar o fluxo completo (ver diagrama em guzula-switch/CLAUDE.md).
 */
@Service
public class PosService implements ChannelResponder {

    @Override
    public String channel() {
        return "POS";
    }

    @Override
    public void sendResponse(CanonicalTransaction transaction) {
        throw new UnsupportedOperationException("TODO: portar pos.service.ts");
    }

    public void handleInbound(byte[] raw) {
        throw new UnsupportedOperationException("TODO: portar pos.service.ts");
    }
}
