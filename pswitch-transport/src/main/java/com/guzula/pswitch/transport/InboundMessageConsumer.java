package com.guzula.pswitch.transport;

/**
 * Callback usado pelo transporte para despachar uma mensagem sem expor tipos do Netty.
 */
@FunctionalInterface
public interface InboundMessageConsumer {

    void accept(String connectionId, byte[] payload);
}
