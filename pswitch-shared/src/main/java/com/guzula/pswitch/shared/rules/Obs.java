package com.guzula.pswitch.shared.rules;

/**
 * Tabela central de regras de negação/observação usadas por todo o sistema para popular {@code
 * CanonicalTransaction.Response} via {@code TableResponseService.populateResponseFromRule}.
 * Referência: obs.ts (guzula-switch).
 *
 * <p>Adicione aqui novas regras conforme os demais módulos forem sendo portados — nunca duplique um
 * código de observação/resposta diretamente em um serviço.
 */
public final class Obs {

  private Obs() {}

  public static final Rule RULE_999_UNREGISTERED_TERMINAL =
      new Rule(999, "Terminal não cadastrado", "96");

  public static final Rule RULE_999_INTERNAL_PROCESSING_ERROR =
      new Rule(999, "Erro interno durante o processamento da transação", "96");

  public static final Rule RULE_165_TERMINAL_BLOCKED = new Rule(165, "Terminal bloqueado", "57");

  /** Descrição base — quem lança monta a versão com bandeira/produto/subproduto interpolados. */
  public static final Rule RULE_999_FAILED_TO_CONVERT_THE_UNIQUE_PRODUCT =
      new Rule(
          999, "Não foi possível definir o produto da bandeira a partir do produto único", "02");
}
