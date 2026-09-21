package com.guzula.pswitch.shared.port;

/**
 * Porta implementada por um canal capaz de receber um payload completo.
 * Não expõe sockets, conexões ou framing para o módulo do canal.
 */
public interface InboundPayloadHandler {

    String channel();

    void handleInbound(byte[] payload);
}
