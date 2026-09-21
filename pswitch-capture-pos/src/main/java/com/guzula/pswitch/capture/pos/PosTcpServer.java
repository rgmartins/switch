package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.transport.TcpRawServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Component;

/**
 * Liga o servidor TCP genérico ao canal POS durante o ciclo de vida do Spring.
 */
@Component
public class PosTcpServer {

    private final TcpRawServer tcpRawServer;
    private final String host;
    private final int port;

    public PosTcpServer(
            PosService posService,
            @Value("${pswitch.pos.tcp.host:0.0.0.0}") String host,
            @Value("${pswitch.pos.tcp.port:9000}") int port) {
        this.tcpRawServer = new TcpRawServer(posService::handleInbound);
        this.host = host;
        this.port = port;
    }

    @PostConstruct
    public void start() {
        tcpRawServer.start(host, port);
        System.out.println(AnsiOutput.toString(
                AnsiColor.BRIGHT_CYAN,
                "Servidor TCP do POS ouvindo em " + host + ":" + port,
                AnsiColor.DEFAULT));
    }

    @PreDestroy
    public void stop() {
        tcpRawServer.stop();
    }
}
