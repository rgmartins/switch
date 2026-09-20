package com.guzula.pswitch.external.tarifas;

import org.springframework.stereotype.Service;

/**
 * Referência: tarifas.service.ts + tarifas-builder.ts + tarifas-message.ts (guzula-switch).
 * Protocolo sequencial ASCII — usa o CobolCodec (pswitch-shared).
 * TODO: portar construção/serialização da mensagem de tarifas.
 */
@Service
public class TarifasService {

    public Object consult(Object canonicalTransaction) {
        throw new UnsupportedOperationException("TODO: portar tarifas.service.ts");
    }
}
