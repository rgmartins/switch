package com.guzula.pswitch.nucleo;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;

/**
 * Porta implementada por cada módulo de captura/canal (ex.: pswitch-capture-pos)
 * para receber de volta a resposta roteada pelo NucleoService, sem que o
 * nucleo dependa diretamente do módulo de captura.
 *
 * Referência: NucleoService.handleResponse -> PosService.sendResponseToPOS (guzula-switch).
 */
public interface ChannelResponder {

    /** Identificador do canal de origem (ex.: "POS"). */
    String channel();

    void sendResponse(CanonicalTransaction transaction);
}
