package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Envia requisições ao HSM e correlaciona cada resposta pelo header. */
@Component
public final class HsmRequestManager {

  private static final Logger LOGGER = Logger.getLogger(HsmRequestManager.class.getName());
  private static final int HEADER_LENGTH = 4;
  private static final int MAX_HEADERS = 10_000;

  private final OutboundPayloadSender payloadSender;
  private final Duration responseTimeout;
  private final AtomicInteger nextHeader = new AtomicInteger();
  private final ConcurrentHashMap<String, CompletableFuture<byte[]>> pendingRequests =
      new ConcurrentHashMap<>();

  public HsmRequestManager(
      OutboundPayloadSender payloadSender,
      @Value("${pswitch.hsm.response-timeout:5s}") Duration responseTimeout) {
    if (responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()) {
      throw new IllegalArgumentException("Timeout de resposta do HSM deve ser maior que zero");
    }
    this.payloadSender = payloadSender;
    this.responseTimeout = responseTimeout;
  }

  public byte[] sendAndWait(String requestCommand, String responseCommand, byte[] commandPayload) {
    PendingRequest pending = registerPendingRequest();
    try {
      byte[] request = addHeader(pending.header(), commandPayload);
      payloadSender.send(HsmService.CONNECTION_NAME, request);
      LOGGER.info(
          () -> "Comando " + requestCommand + " enviado ao HSM: header=" + pending.header());
      return pending.future().get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException exception) {
      throw new IllegalStateException(
          "Timeout aguardando resposta " + responseCommand + " do HSM: header=" + pending.header(),
          exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Espera pela resposta " + responseCommand + " foi interrompida", exception);
    } catch (ExecutionException exception) {
      throw new IllegalStateException(
          "Falha aguardando resposta " + responseCommand + " do HSM", exception.getCause());
    } finally {
      pendingRequests.remove(pending.header(), pending.future());
    }
  }

  private byte[] addHeader(String header, byte[] commandPayload) {
    byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
    byte[] request = new byte[headerBytes.length + commandPayload.length];
    System.arraycopy(headerBytes, 0, request, 0, headerBytes.length);
    System.arraycopy(commandPayload, 0, request, headerBytes.length, commandPayload.length);
    return request;
  }

  public void complete(byte[] payload) {
    if (payload.length < HEADER_LENGTH) {
      throw new IllegalArgumentException("Resposta HSM menor que o header de quatro posições");
    }

    String header = new String(payload, 0, HEADER_LENGTH, StandardCharsets.US_ASCII);
    CompletableFuture<byte[]> pending = pendingRequests.remove(header);
    if (pending == null) {
      LOGGER.warning(() -> "Resposta HSM sem requisição pendente: header=" + header);
      return;
    }
    pending.complete(payload.clone());
  }

  private PendingRequest registerPendingRequest() {
    for (int attempt = 0; attempt < MAX_HEADERS; attempt++) {
      int value = nextHeader.getAndUpdate(current -> (current + 1) % MAX_HEADERS);
      String header = String.format(Locale.ROOT, "%04d", value);
      CompletableFuture<byte[]> future = new CompletableFuture<>();
      if (pendingRequests.putIfAbsent(header, future) == null) {
        return new PendingRequest(header, future);
      }
    }
    throw new IllegalStateException("Não há headers HSM disponíveis");
  }

  private record PendingRequest(String header, CompletableFuture<byte[]> future) {}
}
