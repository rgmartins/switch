package com.guzula.pswitch.app.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TcpResponseGatewayTest {

  @Test
  void sendsPayloadToTheIdentifiedConnection() {
    TcpResponseGateway gateway = new TcpResponseGateway();
    AtomicReference<byte[]> sentPayload = new AtomicReference<>();
    byte[] payload = "dados".getBytes(StandardCharsets.UTF_8);

    gateway.connected("connection-1", sentPayload::set);
    gateway.send("connection-1", payload);

    assertArrayEquals(payload, sentPayload.get());
  }

  @Test
  void rejectsSendWhenConnectionIsNotActive() {
    TcpResponseGateway gateway = new TcpResponseGateway();

    assertThrows(
        IllegalStateException.class, () -> gateway.send("missing-connection", new byte[0]));
  }
}
