package com.guzula.pswitch.comum;

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

  public ComumService(TerminalService terminalService) {
    this.terminalService = terminalService;
  }

  public CanonicalTransaction process(CanonicalTransaction canonical) {
    terminalService.populate(canonical);
    return canonical;
  }
}
