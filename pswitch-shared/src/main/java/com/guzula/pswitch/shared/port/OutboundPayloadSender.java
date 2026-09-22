package com.guzula.pswitch.shared.port;

/** Porta de saída para enviar um payload por uma conexão previamente identificada. */
public interface OutboundPayloadSender {

  void send(String connectionId, byte[] payload);
}
