package com.guzula.pswitch.app.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pswitch.tcp")
public record TcpServersProperties(List<Listener> listeners) {

  public TcpServersProperties {
    listeners = listeners == null ? List.of() : List.copyOf(listeners);
  }

  public record Listener(String channel, String host, int port) {}
}
