package com.guzula.pswitch.transport;

/**
 * Pool de conexões TCP outbound (fala com a bandeira / processador de destino).
 * Referência: TcpOutboundPool em src/shared/transporters/tcp-outbound.pool.ts (guzula-switch).
 *
 * TODO: portar gerenciamento de conexões (reconexão, timeout, correlação de
 * request/response — no original isso é feito via Redis chaveado por RRN).
 */
public class TcpOutboundPool {

    public void connect(String host, int port) {
        throw new UnsupportedOperationException("TODO: portar tcp-outbound.pool.ts");
    }

    public void send(byte[] payload) {
        throw new UnsupportedOperationException("TODO: portar tcp-outbound.pool.ts");
    }
}
