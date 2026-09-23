package com.guzula.pswitch.app.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TcpMessageDispatcherTest {

  @Test
  void routesPayloadToConfiguredHandler() throws Exception {
    CompletableFuture<String> receivedConnection = new CompletableFuture<>();
    CompletableFuture<byte[]> receivedPayload = new CompletableFuture<>();
    InboundPayloadHandler handler =
        new InboundPayloadHandler() {
          @Override
          public String handlerName() {
            return "POS";
          }

          @Override
          public void handleInbound(String connectionId, byte[] payload) {
            receivedConnection.complete(connectionId);
            receivedPayload.complete(payload);
          }
        };

    TcpMessageDispatcher dispatcher = new TcpMessageDispatcher(List.of(handler));
    try {
      byte[] payload = "mensagem".getBytes(StandardCharsets.UTF_8);
      dispatcher.route("pos").accept("connection-1", payload);

      assertEquals("connection-1", receivedConnection.get(3, TimeUnit.SECONDS));
      assertArrayEquals(payload, receivedPayload.get(3, TimeUnit.SECONDS));
    } finally {
      dispatcher.stop();
    }
  }

  @Test
  void rejectsUnknownHandler() {
    TcpMessageDispatcher dispatcher = new TcpMessageDispatcher(List.of());
    try {
      assertThrows(IllegalStateException.class, () -> dispatcher.route("missing"));
    } finally {
      dispatcher.stop();
    }
  }
}
