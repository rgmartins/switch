package com.guzula.pswitch.comunicacao;

import com.guzula.pswitch.transport.TcpRawServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cria os listeners TCP configurados e conecta cada um ao canal correspondente. */
@Component
@EnableConfigurationProperties(TcpServersProperties.class)
public class TcpServersConfiguration {

  private final TcpServersProperties properties;
  private final TcpMessageDispatcher dispatcher;
  private final TcpResponseGateway responseGateway;
  private final List<TcpRawServer> servers = new ArrayList<>();

  public TcpServersConfiguration(
      TcpServersProperties properties,
      TcpMessageDispatcher dispatcher,
      TcpResponseGateway responseGateway) {
    this.properties = properties;
    this.dispatcher = dispatcher;
    this.responseGateway = responseGateway;
  }

  @PostConstruct
  public void start() {
    try {
      for (TcpServersProperties.Listener listener : properties.listeners()) {
        TcpRawServer server =
            new TcpRawServer(dispatcher.route(listener.handler()), responseGateway);
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
  }
}
