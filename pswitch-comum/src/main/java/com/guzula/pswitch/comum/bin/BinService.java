package com.guzula.pswitch.comum.bin;

import com.guzula.pswitch.registry.bin.BinConfig;
import com.guzula.pswitch.registry.bin.BinRegistry;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/** Localiza a faixa do cartão e popula no canônico os dados cadastrados para o BIN. */
@Service
public class BinService {
  private final BinRegistry binRegistry;

  public BinService(BinRegistry binRegistry) {
    this.binRegistry = binRegistry;
  }

  public BinConfig getByCard(CanonicalTransaction canonical) {
    String pan = cardNumber(canonical);
    BinConfig config =
        binRegistry
            .findByPan(pan)
            .orElseThrow(() -> new IllegalStateException("Faixa de BIN não cadastrada: " + pan));
    populateCard(canonical.getCard(), config);
    return config;
  }

  private String cardNumber(CanonicalTransaction canonical) {
    if (canonical.getCard() == null || canonical.getCard().getCardNumber() == null) {
      throw new IllegalStateException("Cartão ausente para consulta da faixa de BIN");
    }

    String pan = canonical.getCard().getCardNumber();
    if (!pan.matches("\\d{13,19}")) {
      throw new IllegalStateException("Cartão inválido para consulta da faixa de BIN");
    }
    return pan;
  }

  private void populateCard(CanonicalTransaction.Card card, BinConfig config) {
    card.setIsForeign(config.isForeign());
    card.setIsCorporate(config.isCorporate());
    card.setIsCredit(config.isCredit());
    card.setIsDebit(config.isDebit());
    card.setIsDebitMaster(config.isDebitMaster());
    card.setProduct(config.product());
    card.setCountry(config.country());

    CanonicalTransaction.Card.CardBrand cardBrand = card.getCardBrand();
    if (cardBrand == null) {
      cardBrand = new CanonicalTransaction.Card.CardBrand();
      card.setCardBrand(cardBrand);
    }
    cardBrand.setAuthorization(String.valueOf(config.cardBrandAuthorization()));
    cardBrand.setRegister(String.valueOf(config.cardBrandRegister()));
    cardBrand.setSettlement(String.valueOf(config.cardBrandSettlement()));
  }
}
