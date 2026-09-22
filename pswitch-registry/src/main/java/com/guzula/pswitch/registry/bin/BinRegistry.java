package com.guzula.pswitch.registry.bin;

/**
 * Contrato do registry de BINs (faixas de cartão). Referência: bin-registry.interface.ts
 * (guzula-switch).
 */
public interface BinRegistry {

  Object findByBin(String bin);
}
