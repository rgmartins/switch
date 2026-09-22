package com.guzula.pswitch.transport;

import java.util.function.Consumer;

/**
 * Notifica a abertura e o fechamento de conexões sem expor o Channel do Netty.
 */
public interface TcpConnectionListener {

    void connected(String connectionId, Consumer<byte[]> sender);

    void disconnected(String connectionId);
}
