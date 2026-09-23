package com.guzula.pswitch.registry.terminal;

import java.util.Optional;

/**
 * Contrato do registry de terminais. Referência: terminal-registry.interface.ts (guzula-switch).
 */
public interface TerminalRegistry {

  Optional<TerminalConfig> findByTerminalId(String terminalId);
}
