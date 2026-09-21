package com.guzula.pswitch.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "pswitch.tcp")
public record TcpServersProperties(List<Listener> listeners) {

    public TcpServersProperties {
        listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    public record Listener(String channel, String host, int port) {
    }
}
