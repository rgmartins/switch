package com.guzula.pswitch.comunicacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do processo de comunicação — roda separado do switch (ver {@code pswitch-app}).
 * Component scan restrito a {@code com.guzula.pswitch.comunicacao}: só as classes de conexão (TCP +
 * pontes de fila) sobem aqui, nunca lógica de negócio (que nem está na classpath deste módulo).
 */
@SpringBootApplication(scanBasePackages = "com.guzula.pswitch.comunicacao")
public class ComunicacaoApplication {

  public static void main(String[] args) {
    SpringApplication.run(ComunicacaoApplication.class, args);
  }
}
