package com.guzula.pswitch.app.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pswitch.tcp")
public record TcpServersProperties(List<Listener> listeners, List<Client> clients) {

  public TcpServersProperties {
    listeners = listeners == null ? List.of() : List.copyOf(listeners);
    clients = clients == null ? List.of() : List.copyOf(clients);
  }

  public record Listener(String channel, String host, int port) {}

  public record Client(String channel, String host, int port, long reconnectDelayMilliseconds) {}
}
