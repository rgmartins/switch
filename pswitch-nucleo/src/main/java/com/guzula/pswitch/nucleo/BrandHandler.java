package com.guzula.pswitch.nucleo;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;

/**
 * Porta implementada por cada módulo de bandeira (ex.: pswitch-brand-visa). O NucleoService
 * descobre os handlers registrados no contexto Spring (sem depender diretamente de
 * pswitch-brand-visa, evitando ciclo de módulos).
 *
 * <p>Referência: NucleoService.route-by-cardBrand.authorization (guzula-switch).
 */
public interface BrandHandler {

  /** Identificador da bandeira que este handler atende (ex.: "VISA"). */
  String brand();

  void authorize(CanonicalTransaction transaction);
}
