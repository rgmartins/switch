package com.guzula.pswitch.comunicacao;

import com.guzula.pswitch.transport.TcpOutboundPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Disponibiliza os componentes genéricos de transporte TCP para a aplicação. */
@Configuration
public class TcpTransportConfiguration {

  @Bean(destroyMethod = "stop")
  TcpOutboundPool tcpOutboundPool() {
    return new TcpOutboundPool();
  }
}
