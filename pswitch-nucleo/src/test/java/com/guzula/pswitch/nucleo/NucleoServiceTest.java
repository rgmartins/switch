package com.guzula.pswitch.nucleo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.comum.bin.BinService;
import com.guzula.pswitch.comum.keyblock.KeyblockService;
import com.guzula.pswitch.comum.tableproductunique.TableProductUniqueService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.comum.terminal.TerminalService;
import com.guzula.pswitch.external.hsm.HsmService;
import com.guzula.pswitch.nucleo.regras.RegrasService;
import com.guzula.pswitch.registry.bin.BinConfig;
import com.guzula.pswitch.registry.keyblock.KeyblockConfig;
import com.guzula.pswitch.registry.terminal.TerminalConfig;
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
            mock(HsmService.class),
            new TableProductUniqueService(key -> Optional.empty()));
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            comumService,
            new RegrasService(),
            new TableResponseService(),
            fakeBrandHandlers(List.of()),
            fakeChannelResponders("POS", sentResponses));

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
            mock(HsmService.class),
            new TableProductUniqueService(key -> Optional.empty()));
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            comumService,
            new RegrasService(),
            new TableResponseService(),
            fakeBrandHandlers(List.of()),
            fakeChannelResponders("POS", sentResponses));

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

  @Test
  void blockedTerminalBecomesDenialResponseRoutedToOriginChannel() {
    TerminalConfig blockedTerminal =
        new TerminalConfig(
            "mongo-id",
            "00891592",
            "Estabelecimento Comercial Exemplo",
            10169548130001L,
            42L,
            54321L,
            9876L,
            998877L,
            665544L,
            new TerminalConfig.Address(
                "Avenida das Nações",
                "450",
                "Bloco B",
                "Centro",
                "Barueri",
                "06454-000",
                "SP",
                "BRA"),
            "j",
            "10169548130001",
            "solucao_pos",
            true,
            true,
            true);
    TerminalService terminalService =
        new TerminalService(terminalId -> Optional.of(blockedTerminal));
    KeyblockService keyblockService = mock(KeyblockService.class);
    when(keyblockService.getSourceKey(any()))
        .thenReturn(
            new KeyblockConfig(
                "mongo-key-id",
                "fffff17001",
                "2026-01-01T00:00:00Z",
                "0123456789ABCDEFFEDCBA9876543210",
                "",
                ""));
    ComumService comumService =
        new ComumService(
            terminalService,
            mock(BinService.class),
            keyblockService,
            mock(HsmService.class),
            new TableProductUniqueService(key -> Optional.empty()));
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            comumService,
            new RegrasService(),
            new TableResponseService(),
            fakeBrandHandlers(List.of()),
            fakeChannelResponders("POS", sentResponses));

    CanonicalTransaction canonical = canonicalFromChannel("POS");

    assertDoesNotThrow(() -> nucleoService.processTransaction(canonical));

    CanonicalTransaction.Response response = canonical.getResponse();
    assertNotNull(response);
    assertEquals("57", response.getResponseCode());
    assertEquals(165, response.getObs().getCode());
    assertEquals("Terminal bloqueado", response.getObs().getDescription());
    assertEquals(1, sentResponses.size());
  }

  @Test
  void routesToMatchingBrandHandlerAfterRulesPass() {
    List<CanonicalTransaction> authorized = new ArrayList<>();
    BrandHandler visaHandler =
        new BrandHandler() {
          @Override
          public String brand() {
            return "1";
          }

          @Override
          public void authorize(CanonicalTransaction transaction) {
            authorized.add(transaction);
          }
        };
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            approvedComumService(),
            new RegrasService(),
            new TableResponseService(),
            fakeBrandHandlers(List.of(visaHandler)),
            fakeChannelResponders("POS", sentResponses));

    CanonicalTransaction canonical = canonicalWithVisaCard("POS");

    assertDoesNotThrow(() -> nucleoService.processTransaction(canonical));

    assertEquals(1, authorized.size());
    assertSame(canonical, authorized.get(0));
    assertNull(canonical.getResponse());
    // Sucesso não responde na hora — a resposta da bandeira chega depois, assincronamente.
    assertEquals(0, sentResponses.size());
  }

  @Test
  void unprocessedBrandBecomesDenialResponseRoutedToOriginChannel() {
    List<CanonicalTransaction> sentResponses = new ArrayList<>();
    NucleoService nucleoService =
        new NucleoService(
            approvedComumService(),
            new RegrasService(),
            new TableResponseService(),
            fakeBrandHandlers(List.of()), // nenhum BrandHandler registrado
            fakeChannelResponders("POS", sentResponses));

    CanonicalTransaction canonical = canonicalWithVisaCard("POS");

    assertDoesNotThrow(() -> nucleoService.processTransaction(canonical));

    CanonicalTransaction.Response response = canonical.getResponse();
    assertNotNull(response);
    assertEquals("02", response.getResponseCode());
    assertEquals(999, response.getObs().getCode());
    assertEquals("Bandeira não processada pelo sistema", response.getObs().getDescription());
    assertEquals(1, sentResponses.size());
  }

  /**
   * Terminal cadastrado e não bloqueado + BIN Visa cadastrado — chega até o roteamento por
   * bandeira.
   */
  private static ComumService approvedComumService() {
    TerminalConfig terminal =
        new TerminalConfig(
            "mongo-id",
            "00891592",
            "Estabelecimento Comercial Exemplo",
            10169548130001L,
            42L,
            54321L,
            9876L,
            998877L,
            665544L,
            new TerminalConfig.Address(
                "Avenida das Nações",
                "450",
                "Bloco B",
                "Centro",
                "Barueri",
                "06454-000",
                "SP",
                "BRA"),
            "j",
            "10169548130001",
            "solucao_pos",
            true,
            true,
            false);
    TerminalService terminalService = new TerminalService(terminalId -> Optional.of(terminal));

    KeyblockService keyblockService = mock(KeyblockService.class);
    when(keyblockService.getSourceKey(any()))
        .thenReturn(
            new KeyblockConfig(
                "mongo-key-id",
                "fffff17001",
                "2026-01-01T00:00:00Z",
                "0123456789ABCDEFFEDCBA9876543210",
                "",
                ""));

    BinConfig bin =
        new BinConfig(
            "mongo-bin-id",
            "4000000000000000",
            "4999999999999999",
            "Visa",
            1,
            "BR",
            1,
            1,
            1,
            false,
            true,
            false,
            false,
            false,
            "credit");
    BinService binService = new BinService(pan -> Optional.of(bin));

    return new ComumService(
        terminalService,
        binService,
        keyblockService,
        mock(HsmService.class),
        new TableProductUniqueService(key -> Optional.empty()));
  }

  private static CanonicalTransaction canonicalWithVisaCard(String channel) {
    CanonicalTransaction canonical = canonicalFromChannel(channel);
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4000000000000002");
    canonical.setCard(card);
    CanonicalTransaction.Security.Ksn ksn = new CanonicalTransaction.Security.Ksn();
    ksn.setBdkIndicator("fffff17001");
    CanonicalTransaction.Security security = new CanonicalTransaction.Security();
    security.setKsn(ksn);
    canonical.setSecurity(security);
    return canonical;
  }

  private static CanonicalTransaction canonicalFromChannel(String channel) {
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setTerminalId("00000000");
    CanonicalTransaction.Communication communication = new CanonicalTransaction.Communication();
    communication.setChannel(channel);
    communication.setSocketId("connection-1");
    canonical.setCommunication(communication);
    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setProduct(new CanonicalTransaction.Operation.Product());
    canonical.setOperation(operation);
    return canonical;
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<List<BrandHandler>> fakeBrandHandlers(List<BrandHandler> handlers) {
    ObjectProvider<List<BrandHandler>> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(handlers);
    return provider;
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
