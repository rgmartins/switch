package com.guzula.pswitch.brand.visa;

import com.guzula.pswitch.nucleo.BrandHandler;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por RRN (via
 * Redis, no original).
 *
 * <p>TODO: portar envio outbound (pswitch-transport) e correlação de resposta.
 */
@Service
public class VisaService implements BrandHandler {

  @Override
  public String brand() {
    return "VISA";
  }

  @Override
  public void authorize(CanonicalTransaction transaction) {
    throw new UnsupportedOperationException("TODO: portar visa.service.ts");
  }
}
