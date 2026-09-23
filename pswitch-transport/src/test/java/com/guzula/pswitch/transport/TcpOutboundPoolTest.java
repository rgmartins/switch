package com.guzula.pswitch.transport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

  private void awaitConnected(TcpOutboundPool pool, String connectionId)
      throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
    while (!pool.isConnected(connectionId) && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
  }
}
