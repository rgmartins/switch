package com.guzula.pswitch.transport;

/**
 * Servidor TCP inbound (recebe conexões de terminais POS / origem).
 * Referência: TcpRawServer em src/shared/transporters/tcp-raw.server.ts (guzula-switch).
 *
 * TODO: montar o bootstrap Netty (EventLoopGroup, ServerBootstrap, pipeline com
 * LengthFieldFramerDecoder/Encoder), configurável a partir de NetworkConfig
 * (hoje em MongoDB no projeto de referência).
 */
public class TcpRawServer {

    public void start(String host, int port) {
        throw new UnsupportedOperationException("TODO: portar bootstrap de tcp-raw.server.ts");
    }

    public void stop() {
        throw new UnsupportedOperationException("TODO: portar shutdown de tcp-raw.server.ts");
    }
}
