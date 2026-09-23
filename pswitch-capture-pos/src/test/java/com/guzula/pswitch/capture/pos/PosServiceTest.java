package com.guzula.pswitch.capture.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PosServiceTest {

  @Test
  void sendsMockG0ToHsmConnection() {
    AtomicReference<String> connection = new AtomicReference<>();
    AtomicReference<byte[]> sentPayload = new AtomicReference<>();
    PosService service =
        new PosService(
            (connectionId, payload) -> {
              connection.set(connectionId);
              sentPayload.set(payload);
            },
            null,
            null,
            null);

    service.sendComandoG0();

    String command = new String(sentPayload.get(), StandardCharsets.US_ASCII);
    assertEquals("HSM", connection.get());
    assertEquals(128, command.length());
    assertEquals("9876G0", command.substring(0, 6));
    assertEquals("0123456789ABCDEF", command.substring(93, 109));
  }

  @Test
  void receivesValidG1FromHsmConnection() {
    PosService service = new PosService((connectionId, payload) -> {}, null, null, null);
    byte[] response = "9876G100160123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    service.handleInbound("HSM", response);
  }

  @Test
  void rejectsReceiveWithoutPendingG1() {
    PosService service = new PosService((connectionId, payload) -> {}, null, null, null);

    assertThrows(IllegalStateException.class, service::receiveComandoG0);
  }
}
