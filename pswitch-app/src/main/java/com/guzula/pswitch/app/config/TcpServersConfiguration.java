package com.guzula.pswitch.app.config;

import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.transport.TcpRawServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cria os listeners TCP configurados e conecta cada um ao canal correspondente.
 */
@Component
@EnableConfigurationProperties(TcpServersProperties.class)
public class TcpServersConfiguration {

    private final TcpServersProperties properties;
    private final Map<String, InboundPayloadHandler> handlers;
    private final List<TcpRawServer> servers = new ArrayList<>();

    public TcpServersConfiguration(
            TcpServersProperties properties,
            List<InboundPayloadHandler> handlers) {
        this.properties = properties;
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                handler -> normalize(handler.channel()),
                Function.identity()));
    }

    @PostConstruct
    public void start() {
        try {
            for (TcpServersProperties.Listener listener : properties.listeners()) {
                InboundPayloadHandler handler = handlers.get(normalize(listener.channel()));
                if (handler == null) {
                    throw new IllegalStateException(
                            "Nenhum handler encontrado para o canal " + listener.channel());
                }

                TcpRawServer server = new TcpRawServer(handler::handleInbound);
                server.start(listener.host(), listener.port());
                servers.add(server);

                System.out.println(AnsiOutput.toString(
                        AnsiColor.BRIGHT_CYAN,
                        "Servidor TCP de " + listener.channel() + " ouvindo em "
                                + listener.host() + ":" + listener.port(),
                        AnsiColor.DEFAULT));
            }
        } catch (RuntimeException exception) {
            stop();
            throw exception;
        }
    }

    @PreDestroy
    public void stop() {
        for (int index = servers.size() - 1; index >= 0; index--) {
            servers.get(index).stop();
        }
        servers.clear();
    }

    private static String normalize(String channel) {
        return channel.toUpperCase(Locale.ROOT);
    }
}
