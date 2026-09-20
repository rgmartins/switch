package com.guzula.pswitch.registry.keyblock;

/**
 * Contrato do registry de keyblocks.
 * Referência: keyblock-registry.interface.ts (guzula-switch).
 */
public interface KeyblockRegistry {

    Object findByKeyblockId(String keyblockId);
}
