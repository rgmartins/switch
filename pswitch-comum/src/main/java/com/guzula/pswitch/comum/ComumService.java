package com.guzula.pswitch.comum;

import com.guzula.pswitch.comum.keyblock.KeyblockService;
import com.guzula.pswitch.comum.terminal.TerminalService;
import com.guzula.pswitch.external.hsm.HsmService;
import com.guzula.pswitch.registry.keyblock.KeyblockConfig;
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
  private final HsmService hsmService;

  public ComumService(
      TerminalService terminalService, KeyblockService keyblockService, HsmService hsmService) {
    this.terminalService = terminalService;
    this.keyblockService = keyblockService;
    this.hsmService = hsmService;
  }

  public CanonicalTransaction process(CanonicalTransaction canonical) {
    terminalService.populate(canonical);
    KeyblockConfig sourceKey = keyblockService.getSourceKey(canonical);
    hsmService.decryptCardData(canonical, sourceKey.keyblock1());
    return canonical;
  }
}
