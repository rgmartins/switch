package com.guzula.pswitch.nucleo;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
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

  public void route(CanonicalTransaction transaction) {
    throw new UnsupportedOperationException("TODO: portar nucleo.service.ts");
  }

  public void handleResponse(CanonicalTransaction transaction) {
    throw new UnsupportedOperationException("TODO: portar nucleo.service.ts");
  }
}
