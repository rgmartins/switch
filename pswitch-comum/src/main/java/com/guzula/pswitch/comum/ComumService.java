package com.guzula.pswitch.comum;

import com.guzula.pswitch.comum.keyblock.KeyblockService;
import com.guzula.pswitch.comum.terminal.TerminalService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/**
 * Orquestra a etapa comum a todos os canais antes do roteamento por bandeira: popula os registries
 * (terminal/keyblock/bin), aciona HSM e antifraude. Referência: comum.service.ts (guzula-switch).
 */
@Service
public class ComumService {

  private final TerminalService terminalService;
  private final KeyblockService keyblockService;

  public ComumService(TerminalService terminalService, KeyblockService keyblockService) {
    this.terminalService = terminalService;
    this.keyblockService = keyblockService;
  }

  public CanonicalTransaction process(CanonicalTransaction canonical) {
    terminalService.populate(canonical);
    keyblockService.getSourceKey(canonical);
    return canonical;
  }
}
