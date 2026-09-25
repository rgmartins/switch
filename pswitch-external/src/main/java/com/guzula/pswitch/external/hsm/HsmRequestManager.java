package com.guzula.pswitch.external.hsm;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Usado pelo switch ({@link HsmService}) para mandar um comando ao HSM e esperar a resposta — só
 * fala com o Redis, nunca com socket. Publica o pedido já pronto (com header) na fila {@code
 * hsm:pedidos}; quem tem a conexão de verdade é o {@code HsmConnectionBridge}, em
 * pswitch-comunicacao, rodando num processo separado.
 *
 * <p>Mora aqui (não em pswitch-comunicacao) de propósito: o switch (pswitch-app) não deve ter
 * nenhuma dependência de pswitch-comunicacao — essa classe só precisa do Redis, então fica junto da
 * lógica de negócio que a usa. Ver docs/arquitetura/topologia-implantacao.md.
 */
@Component
public final class HsmRequestManager {

  private static final Logger LOGGER = Logger.getLogger(HsmRequestManager.class.getName());
  private static final int MAX_HEADERS = 10_000;
  private static final String CLAIM_KEY_PREFIX = "hsm:pendente:";
  private static final String RESPONSE_KEY_PREFIX = "hsm:resposta:";
  static final String REQUEST_QUEUE_KEY = "hsm:pedidos";
  private static final Duration CLAIM_TTL_BUFFER = Duration.ofSeconds(5);

  private final StringRedisTemplate redisTemplate;
  private final Duration responseTimeout;
  private final AtomicInteger nextHeader = new AtomicInteger();

  public HsmRequestManager(
      StringRedisTemplate redisTemplate,
      @Value("${pswitch.hsm.response-timeout:5s}") Duration responseTimeout) {
    if (responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()) {
      throw new IllegalArgumentException("Timeout de resposta do HSM deve ser maior que zero");
    }
    this.redisTemplate = redisTemplate;
    this.responseTimeout = responseTimeout;
  }

  public byte[] sendAndWait(String requestCommand, String responseCommand, byte[] commandPayload) {
    String header = claimHeader();
    String responseKey = RESPONSE_KEY_PREFIX + header;
    try {
      byte[] request = addHeader(header, commandPayload);
      redisTemplate.opsForList().rightPush(REQUEST_QUEUE_KEY, HexFormat.of().formatHex(request));
      LOGGER.info(
          () -> "Comando " + requestCommand + " publicado na fila do HSM: header=" + header);

      String hexResponse = redisTemplate.opsForList().leftPop(responseKey, responseTimeout);
      if (hexResponse == null) {
        throw new IllegalStateException(
            "Timeout aguardando resposta " + responseCommand + " do HSM: header=" + header);
      }
      return HexFormat.of().parseHex(hexResponse);
    } finally {
      redisTemplate.delete(responseKey);
      redisTemplate.delete(CLAIM_KEY_PREFIX + header);
    }
  }

  private byte[] addHeader(String header, byte[] commandPayload) {
    byte[] headerBytes = header.getBytes(StandardCharsets.US_ASCII);
    byte[] request = new byte[headerBytes.length + commandPayload.length];
    System.arraycopy(headerBytes, 0, request, 0, headerBytes.length);
    System.arraycopy(commandPayload, 0, request, headerBytes.length, commandPayload.length);
    return request;
  }

  private String claimHeader() {
    Duration claimTtl = responseTimeout.plus(CLAIM_TTL_BUFFER);
    for (int attempt = 0; attempt < MAX_HEADERS; attempt++) {
      int value = nextHeader.getAndUpdate(current -> (current + 1) % MAX_HEADERS);
      String header = String.format(Locale.ROOT, "%04d", value);
      Boolean claimed =
          redisTemplate.opsForValue().setIfAbsent(CLAIM_KEY_PREFIX + header, "1", claimTtl);
      if (Boolean.TRUE.equals(claimed)) {
        return header;
      }
    }
    throw new IllegalStateException("Não há headers HSM disponíveis");
  }
}
