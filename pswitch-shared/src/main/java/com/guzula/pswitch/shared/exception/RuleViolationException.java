package com.guzula.pswitch.shared.exception;

import com.guzula.pswitch.shared.rules.Rule;

/**
 * Lançada por qualquer serviço do pipeline de transação (registries, HSM, antifraude, regras de
 * negócio, ...) quando uma regra de negação deve ser aplicada. Não é responsabilidade de quem lança
 * popular a resposta do canônico — isso é feito de forma centralizada pelo orquestrador
 * (NucleoService.processTransaction), que captura esta exceção e chama
 * TableResponseService.populateResponseFromRule com a regra carregada aqui. Referência:
 * RuleViolationError em rule-violation-error.ts (guzula-switch).
 */
public class RuleViolationException extends RuntimeException {

  private final Rule rule;

  public RuleViolationException(Rule rule) {
    super(rule.description());
    this.rule = rule;
  }

  public Rule getRule() {
    return rule;
  }
}
