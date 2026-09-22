package com.guzula.pswitch.registry.terminal;

import org.springframework.stereotype.Service;

/**
 * Referência: terminal-registry.service.ts (guzula-switch). TODO: carregar terminais do MongoDB no
 * startup e popular o canônico.
 */
@Service
public class TerminalRegistryService implements TerminalRegistry {

  @Override
  public Object findByTerminalId(String terminalId) {
    throw new UnsupportedOperationException("TODO: portar terminal-registry.service.ts");
  }
}
