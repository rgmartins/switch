package com.guzula.pswitch.nucleo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.comum.bin.BinService;
import com.guzula.pswitch.comum.keyblock.KeyblockService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.comum.terminal.TerminalService;
import com.guzula.pswitch.external.hsm.HsmService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Cobre o ponto único de conversão de erro/regra de negócio em resposta de negação, e o roteamento
 * dessa resposta de volta ao canal de origem, usado por todos os canais via {@link
 * NucleoService#processTransaction}. Referência: comum.service.ts (guzula-switch) — nenhuma exceção
 * deve escapar deste método.
 */
class NucleoServiceTest {

  @Test
  void ruleViolationFromRegistryBecomesDenialResponseRoutedToOriginChannel() {
    TerminalService terminalService = new TerminalService(terminalId -> Optional.empty());
    ComumService comumService =
        new ComumService(
            terminalService,
            mock(BinService.class),
            mock(KeyblockService.class),
            mock(HsmService.class));
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            comumService, new TableResponseService(), fakeChannelResponders("POS", sentResponses));

    CanonicalTransaction canonical = canonicalFromChannel("POS");

    assertDoesNotThrow(() -> nucleoService.processTransaction(canonical));

    CanonicalTransaction.Response response = canonical.getResponse();
    assertNotNull(response);
    assertEquals("96", response.getResponseCode());
    assertEquals(999, response.getObs().getCode());
    assertEquals("Terminal não cadastrado", response.getObs().getDescription());
    assertEquals(1, sentResponses.size());
    assertSame(canonical, sentResponses.get(0));
  }

  @Test
  void unexpectedExceptionBecomesGenericDenialResponseRoutedToOriginChannel() {
    // Simula uma falha inesperada (não é RuleViolationException) em qualquer ponto do pipeline.
    TerminalService terminalService =
        new TerminalService(
            terminalId -> {
              throw new RuntimeException("Falha inesperada de infraestrutura");
            });
    ComumService comumService =
        new ComumService(
            terminalService,
            mock(BinService.class),
            mock(KeyblockService.class),
            mock(HsmService.class));
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            comumService, new TableResponseService(), fakeChannelResponders("POS", sentResponses));

    CanonicalTransaction canonical = canonicalFromChannel("POS");

    assertDoesNotThrow(() -> nucleoService.processTransaction(canonical));

    CanonicalTransaction.Response response = canonical.getResponse();
    assertNotNull(response);
    assertEquals("96", response.getResponseCode());
    assertEquals(999, response.getObs().getCode());
    assertEquals("Falha inesperada de infraestrutura", response.getError().getMessage());
    assertNotNull(response.getError().getStack());
    assertEquals(1, sentResponses.size());
  }

  private static CanonicalTransaction canonicalFromChannel(String channel) {
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setTerminalId("00000000");
    CanonicalTransaction.Communication communication = new CanonicalTransaction.Communication();
    communication.setChannel(channel);
    communication.setSocketId("connection-1");
    canonical.setCommunication(communication);
    return canonical;
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<List<ChannelResponder>> fakeChannelResponders(
      String channel, List<CanonicalTransaction> sentResponses) {
    ChannelResponder responder =
        new ChannelResponder() {
          @Override
          public String channelName() {
            return channel;
          }

          @Override
          public void sendResponse(CanonicalTransaction transaction) {
            sentResponses.add(transaction);
          }
        };
    ObjectProvider<List<ChannelResponder>> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(List.of(responder));
    return provider;
  }
}
