package com.guzula.pswitch.external.preauth;

import org.springframework.stereotype.Service;

/**
 * Referência: pre-auth.service.ts (guzula-switch) — esqueleto, protocolo ainda não definido no
 * projeto original (aguardando layout, protocolo sequencial ASCII similar a tarifas). TODO:
 * implementar buildRequest / serialize / extractCorrelationKey quando o layout chegar.
 */
@Service
public class PreAuthService {

  public Object requestPreAuth(Object canonicalTransaction) {
    throw new UnsupportedOperationException(
        "TODO: aguardando layout do protocolo (ver TODO.md guzula-switch)");
  }
}
