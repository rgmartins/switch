package com.guzula.pswitch.external.hsm;

import org.springframework.stereotype.Service;

/**
 * Referência: hsm.service.ts + hsm/commands (guzula-switch).
 * TODO: portar comandos HSM (ex.: geração/validação de PIN block, MAC).
 */
@Service
public class HsmService {

    public Object execute(Object command) {
        throw new UnsupportedOperationException("TODO: portar hsm.service.ts");
    }
}
