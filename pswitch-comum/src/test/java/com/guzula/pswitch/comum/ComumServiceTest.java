package com.guzula.pswitch.comum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guzula.pswitch.comum.bin.BinService;
import com.guzula.pswitch.comum.keyblock.KeyblockService;
import com.guzula.pswitch.comum.tableproductunique.TableProductUniqueService;
import com.guzula.pswitch.comum.terminal.TerminalService;
import com.guzula.pswitch.external.hsm.HsmService;
import com.guzula.pswitch.registry.bin.BinConfig;
import com.guzula.pswitch.registry.keyblock.KeyblockConfig;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ComumServiceTest {

  @Test
  void populatesCanonicalFromBinRegistrationEvenWhenThereIsNoPinToTranslate() {
    TerminalService terminalService = mock(TerminalService.class);
    KeyblockService keyblockService = mock(KeyblockService.class);
    HsmService hsmService = mock(HsmService.class);
    KeyblockConfig sourceKey =
        new KeyblockConfig("id", "source", "2026-01-01T00:00:00Z", "source-key", "", "");
    BinConfig bin =
        new BinConfig(
            "id",
            "4000000000000000",
            "4999999999999999",
            "Visa",
            1,
            "BR",
            1,
            2,
            3,
            true,
            false,
            true,
            true,
            true,
            "corporate-credit");
    BinService binService = new BinService(pan -> Optional.of(bin));
    CanonicalTransaction canonical = new CanonicalTransaction();
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4000000000000002");
    canonical.setCard(card);
    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setProduct(new CanonicalTransaction.Operation.Product());
    canonical.setOperation(operation);

    when(keyblockService.getSourceKey(canonical)).thenReturn(sourceKey);

    ComumService service =
        new ComumService(
            terminalService,
            binService,
            keyblockService,
            hsmService,
            new TableProductUniqueService(key -> Optional.empty()));

    assertSame(canonical, service.process(canonical));

    assertEquals("BR", card.getCountry());
    assertEquals("corporate-credit", card.getProduct());
    assertTrue(card.getIsDebit());
    assertFalse(card.getIsCredit());
    assertTrue(card.getIsForeign());
    assertTrue(card.getIsCorporate());
    assertTrue(card.getIsDebitMaster());
    assertEquals("1", card.getCardBrand().getAuthorization());
    assertEquals("2", card.getCardBrand().getRegister());
    assertEquals("3", card.getCardBrand().getSettlement());
    verify(keyblockService, never()).getKey("brand-1");
    verify(hsmService, never()).translatePinBlock(canonical, "source-key", "destination-key");
  }
}
