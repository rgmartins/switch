package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.port.OutboundPayloadSender;
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
 * Envia requisições ao HSM e correlaciona cada resposta pelo header, via Redis.
 *
 * <p>A correlação mora no Redis (não em memória do processo) porque, quando o switch rodar em
 * várias réplicas, a resposta pode ser recebida por uma réplica diferente da que enviou o pedido —
 * ver docs/arquitetura/topologia-implantacao.md. O header do protocolo com o HSM (4 dígitos) dobra
 * como identificador da chave no Redis.
 */
@Component
public final class HsmRequestManager {

  private static final Logger LOGGER = Logger.getLogger(HsmRequestManager.class.getName());
  private static final int HEADER_LENGTH = 4;
  private static final int MAX_HEADERS = 10_000;
  private static final String CLAIM_KEY_PREFIX = "hsm:pendente:";
  private static final String RESPONSE_KEY_PREFIX = "hsm:resposta:";
  private static final Duration CLAIM_TTL_BUFFER = Duration.ofSeconds(5);

  private final OutboundPayloadSender payloadSender;
  private final StringRedisTemplate redisTemplate;
  private final Duration responseTimeout;
  private final AtomicInteger nextHeader = new AtomicInteger();

  public HsmRequestManager(
      OutboundPayloadSender payloadSender,
      StringRedisTemplate redisTemplate,
      @Value("${pswitch.hsm.response-timeout:5s}") Duration responseTimeout) {
    if (responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()) {
      throw new IllegalArgumentException("Timeout de resposta do HSM deve ser maior que zero");
    }
    this.payloadSender = payloadSender;
    this.redisTemplate = redisTemplate;
    this.responseTimeout = responseTimeout;
  }

  public byte[] sendAndWait(String requestCommand, String responseCommand, byte[] commandPayload) {
    String header = claimHeader();
    String responseKey = RESPONSE_KEY_PREFIX + header;
    try {
      byte[] request = addHeader(header, commandPayload);
      payloadSender.send(HsmService.CONNECTION_NAME, request);
      LOGGER.info(() -> "Comando " + requestCommand + " enviado ao HSM: header=" + header);

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

  public void complete(byte[] payload) {
    if (payload.length < HEADER_LENGTH) {
      throw new IllegalArgumentException("Resposta HSM menor que o header de quatro posições");
    }

    String header = new String(payload, 0, HEADER_LENGTH, StandardCharsets.US_ASCII);
    if (Boolean.FALSE.equals(redisTemplate.hasKey(CLAIM_KEY_PREFIX + header))) {
      LOGGER.warning(() -> "Resposta HSM sem requisição pendente: header=" + header);
      return;
    }

    String responseKey = RESPONSE_KEY_PREFIX + header;
    String hexPayload = HexFormat.of().formatHex(payload);
    redisTemplate.opsForList().rightPush(responseKey, hexPayload);
    redisTemplate.expire(responseKey, responseTimeout.plus(CLAIM_TTL_BUFFER));
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
