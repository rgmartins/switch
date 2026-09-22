package com.guzula.pswitch.liquidacao;

import org.springframework.stereotype.Service;

/**
 * Referência: liquidacao-consumer.service.ts (guzula-switch).
 *
 * <p>Nota (TODO.md do projeto original): campo `valor` está como Float — decisão tomada é migrar
 * para inteiro (centavos) em todo o pipeline financeiro. Ao portar, já modelar o valor como long
 * (centavos), sem repetir o float.
 */
@Service
public class LiquidacaoConsumerService {

  public void consume(Object message) {
    throw new UnsupportedOperationException("TODO: portar liquidacao-consumer.service.ts");
  }
}
