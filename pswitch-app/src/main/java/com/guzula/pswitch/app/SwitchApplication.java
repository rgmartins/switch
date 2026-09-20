package com.guzula.pswitch.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada. Component scan cobre com.guzula.pswitch.* (todos os módulos).
 * Referência: src/main.ts + src/app.module.ts (guzula-switch).
 *
 * TODO: subir TcpRawServer (pswitch-transport) no startup, apontando para as
 * portas/hosts hoje configurados via NetworkConfig no MongoDB (projeto original).
 */
@SpringBootApplication(scanBasePackages = "com.guzula.pswitch")
public class SwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwitchApplication.class, args);
    }
}
