package com.guzula.pswitch.comunicacao;

import com.guzula.pswitch.transport.TcpOutboundPool;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Component;

/** Inicia as conexões TCP outbound declaradas na configuração da aplicação. */
@Component
public class TcpClientsConfiguration {

  private final TcpServersProperties properties;
  private final TcpOutboundPool outboundPool;
  private final TcpMessageDispatcher dispatcher;
  private final TcpResponseGateway responseGateway;

  public TcpClientsConfiguration(
      TcpServersProperties properties,
      TcpOutboundPool outboundPool,
      TcpMessageDispatcher dispatcher,
      TcpResponseGateway responseGateway) {
    this.properties = properties;
    this.outboundPool = outboundPool;
    this.dispatcher = dispatcher;
    this.responseGateway = responseGateway;
  }

  @PostConstruct
  public void start() {
    for (TcpServersProperties.Client client : properties.clients()) {
      String channel = client.channel().toUpperCase(Locale.ROOT);
      outboundPool.connect(
          channel,
          client.host(),
          client.port(),
          Duration.ofMillis(client.reconnectDelayMilliseconds()),
          dispatcher.route(client.handler()),
          responseGateway);

      System.out.println(
          AnsiOutput.toString(
              AnsiColor.BRIGHT_CYAN,
              "Cliente TCP de "
                  + channel
                  + " configurado para "
                  + client.host()
                  + ":"
                  + client.port(),
              AnsiColor.DEFAULT));
    }
  }
}
