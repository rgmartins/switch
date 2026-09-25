package com.guzula.pswitch.comunicacao;

import com.guzula.pswitch.transport.TcpOutboundPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disponibiliza os componentes genéricos de transporte TCP para a aplicação.
 *
 * <p>Continua ativa tanto no {@code pswitch-app} (ainda segura POS e Visa, que não migraram pra
 * fila ainda) quanto no {@code pswitch-comunicacao} (segura o HSM, já migrado) — cada processo só
 * abre as portas/conexões que aparecem no seu próprio {@code application.yml}
 * (`pswitch.tcp.listeners`/`clients`). Não precisa de profile: a lista de conexões de cada processo
 * já resolve isso.
 */
@Configuration
public class TcpTransportConfiguration {

  @Bean(destroyMethod = "stop")
  TcpOutboundPool tcpOutboundPool() {
    return new TcpOutboundPool();
  }
}
