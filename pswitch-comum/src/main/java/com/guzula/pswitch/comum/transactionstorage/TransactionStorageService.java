package com.guzula.pswitch.comum.transactionstorage;

import org.springframework.stereotype.Service;

/**
 * Referência: transaction-storage.service.ts (guzula-switch).
 *
 * Nota (TODO.md do projeto original): trocar upsert por create para detectar NSU
 * duplicado explicitamente, e disparar reversal automático (MTI 0420) se a
 * persistência falhar após aprovação da bandeira — ver risco financeiro descrito lá.
 */
@Service
public class TransactionStorageService {

    public void save(Object canonicalTransaction) {
        throw new UnsupportedOperationException("TODO: portar transaction-storage.service.ts");
    }
}
