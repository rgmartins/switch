package com.guzula.pswitch.external.historico;

import org.springframework.stereotype.Service;

/**
 * Referência: historico.service.ts + historico-builder.ts + historico-message.ts (guzula-switch).
 * TODO: portar serialização e envio da mensagem de histórico.
 */
@Service
public class HistoricoService {

  public void send(Object canonicalTransaction) {
    throw new UnsupportedOperationException("TODO: portar historico.service.ts");
  }
}
