package com.guzula.pswitch.registry.keyblock;

import org.springframework.stereotype.Service;

/**
 * Referência: keyblock-registry.service.ts (guzula-switch).
 * TODO: carregar keyblocks do MongoDB no startup.
 */
@Service
public class KeyblockRegistryService implements KeyblockRegistry {

    @Override
    public Object findByKeyblockId(String keyblockId) {
        throw new UnsupportedOperationException("TODO: portar keyblock-registry.service.ts");
    }
}
