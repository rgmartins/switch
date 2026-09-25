package com.guzula.pswitch.comunicacao;

import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import com.guzula.pswitch.transport.TcpConnectionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Gateway central para enviar bytes por qualquer conexão TCP registrada. */
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
