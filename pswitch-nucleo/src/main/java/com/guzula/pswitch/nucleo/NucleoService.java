package com.guzula.pswitch.nucleo;

import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.nucleo.regras.RegrasService;
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
 * <p>TODO: portar armazenamento da transação.
 */
@Service
public class NucleoService {

  private final ComumService comumService;
  private final RegrasService regrasService;
  private final TableResponseService tableResponseService;
  private final List<BrandHandler> brandHandlers;
  private final ObjectProvider<List<ChannelResponder>> channelResponders;

  /**
   * {@code channelResponders} é resolvido de forma preguiçosa (via {@link ObjectProvider}, só
   * dentro de {@link #handleResponse}) porque cada módulo de canal (ex.: PosService) também injeta
   * {@link NucleoService} — resolver a lista aqui no construtor criaria um ciclo de bean do Spring
   * entre NucleoService e cada ChannelResponder. {@code brandHandlers} não tem esse problema
   * (nenhum BrandHandler injeta NucleoService), então é resolvido direto no construtor.
   */
  public NucleoService(
      ComumService comumService,
      RegrasService regrasService,
      TableResponseService tableResponseService,
      List<BrandHandler> brandHandlers,
      ObjectProvider<List<ChannelResponder>> channelResponders) {
    this.comumService = comumService;
    this.regrasService = regrasService;
    this.tableResponseService = tableResponseService;
    this.brandHandlers = brandHandlers;
    this.channelResponders = channelResponders;
  }

  /**
   * Ponto de entrada único do pipeline de transação para todos os canais (POS, TEF, ...). Converte
   * qualquer {@link RuleViolationException} ou falha inesperada numa resposta de negação, em vez de
   * propagar e derrubar a transação.
   *
   * <p>Só chama {@link #handleResponse} no caminho de erro. Quando {@link #route} tem sucesso, ele
   * apenas dispara a mensagem pra bandeira (ex.: Visa) — a resposta de verdade chega depois, de
   * forma assíncrona, via {@code BrandHandler.handleInbound} (correlação ainda TODO em
   * VisaService); responder aqui seria devolver ao canal uma transação sem resultado nenhum.
   */
  public CanonicalTransaction processTransaction(CanonicalTransaction canonical) {
    try {
      comumService.process(canonical);
      regrasService.validate(canonical); // regras de negócio — terminal bloqueado é a primeira
      route(canonical);
      return canonical;
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

  /**
   * Roteia a transação até o {@link BrandHandler} da bandeira (card.cardBrand.authorization).
   * Referência: NucleoService.routeToBrand (guzula-switch).
   */
  public void route(CanonicalTransaction transaction) {
    String brand = transaction.getCard().getCardBrand().getAuthorization();
    BrandHandler handler =
        brandHandlers.stream()
            .filter(candidate -> candidate.brand().equals(brand))
            .findFirst()
            .orElseThrow(() -> new RuleViolationException(Obs.RULE_999_UNPROCESSED_BRAND));
    handler.authorize(transaction);
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
