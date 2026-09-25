package com.guzula.pswitch.comunicacao.hsm;

import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Ponte entre a conexão de verdade com o HSM e as filas do Redis — só existe no processo de
 * comunicação, nunca no switch.
 *
 * <p>Dois sentidos:
 *
 * <ul>
 *   <li>switch → HSM: consome {@code hsm:pedidos} (publicada por {@code HsmRequestManager}, do lado
 *       do switch, em pswitch-external) e manda o payload pela conexão real.
 *   <li>HSM → switch: recebe a resposta pela conexão real ({@link #handleInbound}) e publica em
 *       {@code hsm:resposta:<header>}, lendo o header dos 4 primeiros bytes.
 * </ul>
 */
@Component
public final class HsmConnectionBridge implements InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(HsmConnectionBridge.class.getName());
  private static final String CONNECTION_NAME = "HSM";
  private static final int HEADER_LENGTH = 4;
  private static final String RESPONSE_KEY_PREFIX = "hsm:resposta:";
  // Precisa bater com HsmRequestManager.REQUEST_QUEUE_KEY (pswitch-external) — não pode importar a
  // constante de lá porque pswitch-comunicacao não depende de pswitch-external (evita ciclo).
  private static final String REQUEST_QUEUE_KEY = "hsm:pedidos";
  private static final Duration RESPONSE_KEY_TTL = Duration.ofSeconds(30);
  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

  private final OutboundPayloadSender payloadSender;
  private final StringRedisTemplate redisTemplate;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean running = true;

  public HsmConnectionBridge(
      OutboundPayloadSender payloadSender, StringRedisTemplate redisTemplate) {
    this.payloadSender = payloadSender;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String handlerName() {
    return CONNECTION_NAME;
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    if (payload.length < HEADER_LENGTH) {
      LOGGER.warning(() -> "Resposta HSM menor que o header de quatro posições, descartada");
      return;
    }

    String header = new String(payload, 0, HEADER_LENGTH, StandardCharsets.US_ASCII);
    String responseKey = RESPONSE_KEY_PREFIX + header;
    redisTemplate.opsForList().rightPush(responseKey, HexFormat.of().formatHex(payload));
    redisTemplate.expire(responseKey, RESPONSE_KEY_TTL);
  }

  @PostConstruct
  public void start() {
    worker.execute(this::forwardRequestsLoop);
  }

  private void forwardRequestsLoop() {
    while (running) {
      try {
        String hexRequest = redisTemplate.opsForList().leftPop(REQUEST_QUEUE_KEY, POLL_TIMEOUT);
        if (hexRequest != null) {
          payloadSender.send(CONNECTION_NAME, HexFormat.of().parseHex(hexRequest));
        }
      } catch (RuntimeException exception) {
        if (running) {
          LOGGER.log(Level.WARNING, "Falha lendo a fila de pedidos do HSM", exception);
        }
      }
    }
  }

  @PreDestroy
  public void stop() {
    running = false;
    worker.shutdownNow();
  }
}
