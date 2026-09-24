package com.guzula.pswitch.comum.tableresponse;

import com.guzula.pswitch.shared.SwitchConstants;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.rules.Rule;
import org.springframework.stereotype.Service;

/**
 * Único ponto do sistema responsável por preencher {@code CanonicalTransaction.Response} a partir
 * de uma {@link Rule}. Nenhum outro serviço deve montar um {@code Response} manualmente — apenas
 * lançar {@code RuleViolationException(rule)} e deixar o orquestrador central
 * (NucleoService.processTransaction) chamar este método. Referência: table-response.service.ts
 * (guzula-switch).
 */
@Service
public class TableResponseService {

  private static final String FALLBACK_RESPONSE_CODE = "96";

  /**
   * Popula {@code canonical.response} com base em uma regra global (obs). A tabela de literais por
   * bandeira (MongoDB, ver table-response.service.ts#get) ainda não foi portada — hoje não existe
   * nenhuma entrada de código de resposta cadastrada, então sempre caímos no mesmo fallback que a
   * versão TS usa quando não acha o código na tabela: mensagem da própria regra, {@code
   * messageSource = SYSTEM} (não {@code GENERIC} — esse só se aplica quando a tabela existe mas não
   * tem literal daquela bandeira). TODO: quando a tabela for portada, resolver {@code message} por
   * bandeira/código em vez deste fallback.
   */
  public void populateResponseFromRule(CanonicalTransaction canonical, Rule rule) {
    String responseCode =
        rule.responseCode() != null ? rule.responseCode() : FALLBACK_RESPONSE_CODE;

    CanonicalTransaction.Response response = new CanonicalTransaction.Response();
    response.setWhoResponded(SwitchConstants.WhoResponded.SYSTEM);

    CanonicalTransaction.Response.Obs obs = new CanonicalTransaction.Response.Obs();
    obs.setCode(rule.obsCode());
    obs.setDescription(rule.description());
    response.setObs(obs);

    response.setResponseCode(responseCode);
    response.setIssuerResponseCode(responseCode);
    response.setMessage(rule.description());
    response.setMessageSource(SwitchConstants.MessageDescriptionSource.SYSTEM);
    response.setAuthorizationCode("");
    response.setPaymentAccountReference("");

    CanonicalTransaction.Emv emv = new CanonicalTransaction.Emv();
    response.setEmv(emv);

    canonical.setResponse(response);
  }
}
