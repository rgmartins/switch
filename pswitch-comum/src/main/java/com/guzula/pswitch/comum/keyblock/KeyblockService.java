package com.guzula.pswitch.comum.keyblock;

import com.guzula.pswitch.registry.keyblock.KeyblockConfig;
import com.guzula.pswitch.registry.keyblock.KeyblockRegistry;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/** Localiza a chave BDK de origem indicada pelo KSN da transação. */
@Service
public class KeyblockService {

  private static final Logger LOGGER = Logger.getLogger(KeyblockService.class.getName());

  private final KeyblockRegistry keyblockRegistry;

  public KeyblockService(KeyblockRegistry keyblockRegistry) {
    this.keyblockRegistry = keyblockRegistry;
  }

  public KeyblockConfig getSourceKey(CanonicalTransaction canonical) {
    String keyblockId = bdkIndicator(canonical);
    KeyblockConfig keyblock =
        keyblockRegistry
            .findByKeyblockId(keyblockId)
            .orElseThrow(() -> new IllegalStateException("Chave não cadastrada: " + keyblockId));

    // Diagnóstico temporário do ambiente de simulação. Remover antes de usar chaves reais.
    LOGGER.info(
        () ->
            "Keyblock carregado: id=%s, key=%s, dateTime1=%s, keyblock1=%s, dateTime2=%s, keyblock2=%s"
                .formatted(
                    keyblock.id(),
                    keyblock.key(),
                    keyblock.dateTime1(),
                    keyblock.keyblock1(),
                    keyblock.dateTime2(),
                    keyblock.keyblock2()));
    return keyblock;
  }

  private String bdkIndicator(CanonicalTransaction canonical) {
    if (canonical.getSecurity() == null
        || canonical.getSecurity().getKsn() == null
        || canonical.getSecurity().getKsn().getBdkIndicator() == null
        || canonical.getSecurity().getKsn().getBdkIndicator().isBlank()) {
      throw new IllegalStateException("KSN sem indicador BDK para consulta da chave");
    }
    return canonical.getSecurity().getKsn().getBdkIndicator();
  }
}
