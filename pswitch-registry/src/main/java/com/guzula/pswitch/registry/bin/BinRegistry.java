package com.guzula.pswitch.registry.bin;

import java.util.Optional;

/**
 * Contrato do registry de BINs (faixas de cartão). Referência: bin-registry.interface.ts
 * (guzula-switch).
 */
public interface BinRegistry {

  Optional<BinConfig> findByPan(String pan19);
}
