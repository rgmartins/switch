package com.guzula.pswitch.registry.keyblock;

import java.util.Optional;

/**
 * Contrato do registry de keyblocks. Referência: keyblock-registry.interface.ts (guzula-switch).
 */
public interface KeyblockRegistry {

  Optional<KeyblockConfig> findByKeyblockId(String keyblockId);
}
