package com.guzula.pswitch.app.config;

import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.transport.TcpRawServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cria os listeners TCP configurados e conecta cada um ao canal correspondente. */
@Component
@EnableConfigurationProperties(TcpServersProperties.class)
public class TcpServersConfiguration {

  private final TcpServersProperties properties;
  private final Map<String, InboundPayloadHandler> handlers;
  private final TcpResponseGateway responseGateway;
  private final List<TcpRawServer> servers = new ArrayList<>();
  private final ExecutorService dispatchExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public TcpServersConfiguration(
      TcpServersProperties properties,
      List<InboundPayloadHandler> handlers,
      TcpResponseGateway responseGateway) {
    this.properties = properties;
    this.responseGateway = responseGateway;
    this.handlers =
        handlers.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    handler -> normalize(handler.channel()), Function.identity()));
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

        TcpRawServer server =
            new TcpRawServer(
                (connectionId, payload) ->
                    dispatchExecutor.execute(() -> handler.handleInbound(connectionId, payload)),
                responseGateway);
        server.start(listener.host(), listener.port());
        servers.add(server);

        System.out.println(
            AnsiOutput.toString(
                AnsiColor.BRIGHT_CYAN,
                "Servidor TCP de "
                    + listener.channel()
                    + " ouvindo em "
                    + listener.host()
                    + ":"
                    + listener.port(),
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
    dispatchExecutor.shutdownNow();
  }

  private static String normalize(String channel) {
    return channel.toUpperCase(Locale.ROOT);
  }
}
