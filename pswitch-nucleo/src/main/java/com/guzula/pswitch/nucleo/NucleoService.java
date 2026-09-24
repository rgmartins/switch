package com.guzula.pswitch.nucleo;

import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.exception.RuleViolationException;
import com.guzula.pswitch.shared.rules.Obs;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Roteia a transação canônica por card.cardBrand.authorization até o {@link BrandHandler}
 * correspondente, e a resposta de volta ao {@link ChannelResponder} de origem. Também aciona o
 * armazenamento da transação (pswitch-comum). Referência: nucleo.service.ts (guzula-switch).
 *
 * <p>TODO: injetar List<BrandHandler> e List<ChannelResponder> via Spring, indexar por
 * brand()/channelName() e portar a lógica de roteamento.
 */
@Service
public class NucleoService {

  private final ComumService comumService;
  private final TableResponseService tableResponseService;
  private final ObjectProvider<List<ChannelResponder>> channelResponders;

  /**
   * {@code channelResponders} é resolvido de forma preguiçosa (via {@link ObjectProvider}, só
   * dentro de {@link #handleResponse}) porque cada módulo de canal (ex.: PosService) também injeta
   * {@link NucleoService} — resolver a lista aqui no construtor criaria um ciclo de bean do Spring
   * entre NucleoService e cada ChannelResponder.
   */
  public NucleoService(
      ComumService comumService,
      TableResponseService tableResponseService,
      ObjectProvider<List<ChannelResponder>> channelResponders) {
    this.comumService = comumService;
    this.tableResponseService = tableResponseService;
    this.channelResponders = channelResponders;
  }

  /**
   * Ponto de entrada único do pipeline de transação para todos os canais (POS, TEF, ...).
   * Referência: comum.service.ts#processTransaction (guzula-switch) — fica aqui, e não em
   * ComumService, porque pswitch-nucleo já depende de pswitch-comum (o inverso criaria um ciclo de
   * módulo Maven).
   *
   * <p>Qualquer {@link RuleViolationException} lançada por um serviço do pipeline (registries, HSM,
   * regras de negócio, ...), ou qualquer outra falha inesperada, é convertida aqui — e somente aqui
   * — numa resposta de negação via {@link TableResponseService}, em vez de propagar e derrubar a
   * transação.
   */
  public CanonicalTransaction processTransaction(CanonicalTransaction canonical) {
    try {
      comumService.process(canonical);
    } catch (RuleViolationException e) {
      tableResponseService.populateResponseFromRule(canonical, e.getRule());
    } catch (Exception e) {
      populateInternalError(canonical, e);
    }
    handleResponse(canonical);
    return canonical;
  }

  private void populateInternalError(CanonicalTransaction canonical, Exception e) {
    tableResponseService.populateResponseFromRule(
        canonical, Obs.RULE_999_INTERNAL_PROCESSING_ERROR);
    CanonicalTransaction.Response.Error error = new CanonicalTransaction.Response.Error();
    error.setMessage(e.getMessage());
    error.setStack(stackTraceOf(e));
    canonical.getResponse().setError(error);
  }

  private static String stackTraceOf(Exception e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }

  public void route(CanonicalTransaction transaction) {
    throw new UnsupportedOperationException("TODO: portar nucleo.service.ts");
  }

  /**
   * Devolve a resposta ao canal de origem (POS, TEF, ...), identificado por {@code
   * transaction.communication.channel}. Referência: NucleoService.handleResponse (guzula-switch).
   */
  public void handleResponse(CanonicalTransaction transaction) {
    String channel = transaction.getCommunication().getChannel();
    ChannelResponder responder =
        channelResponders.getObject().stream()
            .filter(candidate -> candidate.channelName().equals(channel))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Nenhum ChannelResponder registrado para o canal: " + channel));
    responder.sendResponse(transaction);
  }
}
