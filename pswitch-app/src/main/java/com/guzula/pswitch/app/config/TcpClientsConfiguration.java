package com.guzula.pswitch.app.config;

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

  public TcpClientsConfiguration(TcpServersProperties properties, TcpOutboundPool outboundPool) {
    this.properties = properties;
    this.outboundPool = outboundPool;
  }

  @PostConstruct
  public void start() {
    for (TcpServersProperties.Client client : properties.clients()) {
      String channel = client.channel().toUpperCase(Locale.ROOT);
      outboundPool.connect(
          channel,
          client.host(),
          client.port(),
          Duration.ofMillis(client.reconnectDelayMilliseconds()));

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
