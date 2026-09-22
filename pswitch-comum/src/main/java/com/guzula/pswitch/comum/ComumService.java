package com.guzula.pswitch.comum;

import org.springframework.stereotype.Service;

/**
 * Orquestra a etapa comum a todos os canais antes do roteamento por bandeira: popula os registries
 * (terminal/keyblock/bin), aciona HSM e antifraude. Referência: comum.service.ts (guzula-switch).
 *
 * <p>TODO: portar o pipeline de enriquecimento do canônico.
 */
@Service
public class ComumService {

  public Object process(Object canonicalTransaction) {
    throw new UnsupportedOperationException("TODO: portar comum.service.ts");
  }
}
