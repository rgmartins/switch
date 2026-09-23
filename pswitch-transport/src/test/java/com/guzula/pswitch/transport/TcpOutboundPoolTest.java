package com.guzula.pswitch.transport;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class TcpOutboundPoolTest {

  @Test
  void connectsToConfiguredEndpointAndClosesCleanly() throws Exception {
    try (ServerSocket server = new ServerSocket(0);
        ExecutorService executor = Executors.newSingleThreadExecutor()) {
      Future<Socket> acceptedConnection = executor.submit(server::accept);
      TcpOutboundPool pool = new TcpOutboundPool();

      try {
        pool.connect("HSM", "127.0.0.1", server.getLocalPort(), Duration.ofMillis(100));

        try (Socket ignored = acceptedConnection.get(3, TimeUnit.SECONDS)) {
          awaitConnected(pool, "HSM");
          assertTrue(pool.isConnected("HSM"));
        }
      } finally {
        pool.stop();
      }

      assertFalse(pool.isConnected("HSM"));
    }
  }

  @Test
  void sendsAndReceivesPayloadThroughConfiguredConnection() throws Exception {
    byte[] request = "PING".getBytes(StandardCharsets.UTF_8);
    byte[] response = "PONG".getBytes(StandardCharsets.UTF_8);

    try (ServerSocket server = new ServerSocket(0);
        ExecutorService executor = Executors.newSingleThreadExecutor()) {
      Future<byte[]> receivedByServer =
          executor.submit(
              () -> {
                try (Socket socket = server.accept()) {
                  DataInputStream input = new DataInputStream(socket.getInputStream());
                  byte[] received = input.readNBytes(input.readUnsignedShort());

                  DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                  output.writeShort(response.length);
                  output.write(response);
                  output.flush();
                  return received;
                }
              });

      CompletableFuture<Consumer<byte[]>> sender = new CompletableFuture<>();
      CompletableFuture<byte[]> receivedByClient = new CompletableFuture<>();
      TcpConnectionListener listener =
          new TcpConnectionListener() {
            @Override
            public void connected(String connectionId, Consumer<byte[]> connectionSender) {
              sender.complete(connectionSender);
            }

            @Override
            public void disconnected(String connectionId) {}
          };

      TcpOutboundPool pool = new TcpOutboundPool();
      try {
        pool.connect(
            "HSM",
            "127.0.0.1",
            server.getLocalPort(),
            Duration.ofMillis(100),
            (connectionId, payload) -> receivedByClient.complete(payload),
            listener);

        sender.get(3, TimeUnit.SECONDS).accept(request);

        assertArrayEquals(request, receivedByServer.get(3, TimeUnit.SECONDS));
        assertArrayEquals(response, receivedByClient.get(3, TimeUnit.SECONDS));
      } finally {
        pool.stop();
      }
    }
  }

  private void awaitConnected(TcpOutboundPool pool, String connectionId)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
    while (!pool.isConnected(connectionId) && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
  }
}
