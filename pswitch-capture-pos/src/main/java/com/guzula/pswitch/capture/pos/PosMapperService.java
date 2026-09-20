package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/**
 * Referência: pos-mapper.service.ts (guzula-switch).
 * Converte a mensagem ISO parseada (parser) para o modelo canônico.
 *
 * TODO: portar mapeamento de DEs para CanonicalTransaction.
 */
@Service
public class PosMapperService {

    public CanonicalTransaction toCanonical(Object parsedPosMessage) {
        throw new UnsupportedOperationException("TODO: portar pos-mapper.service.ts");
    }
}
