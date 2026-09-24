package com.guzula.pswitch.nucleo.regras;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.exception.RuleViolationException;
import com.guzula.pswitch.shared.rules.Obs;
import org.springframework.stereotype.Service;

/**
 * Valida regras de negócio sobre a transação canônica, após ComumService.process. Cada regra lança
 * {@link RuleViolationException} — quem converte isso numa resposta de negação é o orquestrador
 * central (NucleoService.processTransaction), não este serviço. Referência: regras.service.ts
 * (guzula-switch).
 *
 * <p>TODO: portar as demais regras (250, 292, 616, 617, 821, 115) conforme forem necessárias.
 */
@Service
public class RegrasService {

  public void validate(CanonicalTransaction canonical) {
    rule165TerminalBlocked(canonical);
  }

  /** Regra 165: terminal bloqueado. */
  private void rule165TerminalBlocked(CanonicalTransaction canonical) {
    if (!Boolean.TRUE.equals(canonical.getEquipment().getTerminalBlocked())) {
      return;
    }
    throw new RuleViolationException(Obs.RULE_165_TERMINAL_BLOCKED);
  }
}
