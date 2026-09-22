package com.guzula.pswitch.app.config;

import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import com.guzula.pswitch.transport.TcpConnectionListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Mantém a associação entre connectionId e a função de escrita no socket.
 */
@Component
public class TcpResponseGateway implements OutboundPayloadSender, TcpConnectionListener {

    private final Map<String, Consumer<byte[]>> connections = new ConcurrentHashMap<>();

    @Override
    public void connected(String connectionId, Consumer<byte[]> sender) {
        connections.put(connectionId, sender);
    }

    @Override
    public void disconnected(String connectionId) {
        connections.remove(connectionId);
    }

    @Override
    public void send(String connectionId, byte[] payload) {
        Consumer<byte[]> sender = connections.get(connectionId);
        if (sender == null) {
            throw new IllegalStateException("Conexão TCP não está ativa: " + connectionId);
        }
        sender.accept(payload);
    }
}
