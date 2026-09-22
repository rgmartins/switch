package com.guzula.pswitch.registry.bin;

import org.springframework.stereotype.Service;

/**
 * Referência: bin-registry.service.ts / bin.service.ts (guzula-switch). TODO: carregar tabela de
 * BINs do MongoDB no startup.
 */
@Service
public class BinRegistryService implements BinRegistry {

  @Override
  public Object findByBin(String bin) {
    throw new UnsupportedOperationException("TODO: portar bin-registry.service.ts");
  }
}
