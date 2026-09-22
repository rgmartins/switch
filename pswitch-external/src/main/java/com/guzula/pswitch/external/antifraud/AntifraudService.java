package com.guzula.pswitch.external.antifraud;

import org.springframework.stereotype.Service;

/**
 * Referência: antifraud.service.ts / antifraud.builder.ts (guzula-switch). TODO: portar construção
 * de request, chamada ao serviço antifraude e parse da resposta.
 */
@Service
public class AntifraudService {

  public Object analyze(Object canonicalTransaction) {
    throw new UnsupportedOperationException("TODO: portar antifraud.service.ts");
  }
}
