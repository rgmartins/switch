package com.guzula.pswitch.registry.terminal;

/**
 * Contrato do registry de terminais. Referência: terminal-registry.interface.ts (guzula-switch).
 */
public interface TerminalRegistry {

  Object findByTerminalId(String terminalId);
}
