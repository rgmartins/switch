package com.guzula.pswitch.comum.bin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.guzula.pswitch.registry.bin.BinConfig;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BinServiceTest {

  @Test
  void findsRangeAndPopulatesCanonicalCard() {
    AtomicReference<String> requestedPan = new AtomicReference<>();
    BinConfig expected =
        new BinConfig(
            "4158960000000000000",
            "4158960000000000000",
            "4158969999999999999",
            "Cartão de Teste Visa",
            999,
            "BR",
            1,
            1,
            1,
            false,
            true,
            false,
            false,
            false,
            "C");
    BinService service =
        new BinService(
            pan -> {
              requestedPan.set(pan);
              return Optional.of(expected);
            });
    CanonicalTransaction canonical = new CanonicalTransaction();
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4158961234567890");
    canonical.setCard(card);

    assertEquals(expected, service.getByCard(canonical));
    assertEquals("4158961234567890", requestedPan.get());
    assertEquals("1", card.getCardBrand().getAuthorization());
    assertEquals("1", card.getCardBrand().getRegister());
    assertEquals("1", card.getCardBrand().getSettlement());
    assertEquals(true, card.getIsCredit());
    assertEquals("C", card.getProduct());
  }
}
