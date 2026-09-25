package com.guzula.pswitch.comunicacao.pos;

import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Ponte entre as conexões de verdade com os terminais POS e as filas do Redis — só existe no
 * processo de comunicação, nunca no switch.
 *
 * <p>Diferente de HSM/Visa (uma conexão outbound só), o POS é um listener com muitas conexões
 * simultâneas — por isso cada mensagem na fila carrega o {@code connectionId} junto, no formato
 * {@code "<connectionId>|<payload em hex>"}, pra saber de qual terminal veio (ida) ou pra qual
 * terminal mandar (volta). Totalmente burra: não entende o protocolo do POS, só repassa.
 *
 * <ul>
 *   <li>POS → switch: consome a conexão real ({@link #handleInbound}) e publica em {@code
 *       pos:pedidos}.
 *   <li>switch → POS: consome {@code pos:respostas} (publicada pelo {@code PosService}, em
 *       pswitch-capture-pos) e manda pela conexão real do terminal certo.
 * </ul>
 */
@Component
public final class PosConnectionBridge implements InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(PosConnectionBridge.class.getName());
  private static final String CHANNEL_NAME = "POS";
  private static final String SEPARATOR = "|";
  // Precisam bater com as constantes homônimas em PosService (pswitch-capture-pos) — não dá pra
  // importar de lá porque pswitch-comunicacao não depende de pswitch-capture-pos (evita ciclo).
  private static final String REQUEST_QUEUE_KEY = "pos:pedidos";
  private static final String RESPONSE_QUEUE_KEY = "pos:respostas";
  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

  private final OutboundPayloadSender payloadSender;
  private final StringRedisTemplate redisTemplate;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean running = true;

  public PosConnectionBridge(
      OutboundPayloadSender payloadSender, StringRedisTemplate redisTemplate) {
    this.payloadSender = payloadSender;
    this.redisTemplate = redisTemplate;
  }

  @Override
  public String handlerName() {
    return CHANNEL_NAME;
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    String message = connectionId + SEPARATOR + HexFormat.of().formatHex(payload);
    redisTemplate.opsForList().rightPush(REQUEST_QUEUE_KEY, message);
  }

  @PostConstruct
  public void start() {
    worker.execute(this::forwardResponsesLoop);
  }

  private void forwardResponsesLoop() {
    while (running) {
      try {
        String message = redisTemplate.opsForList().leftPop(RESPONSE_QUEUE_KEY, POLL_TIMEOUT);
        if (message != null) {
          int separatorIndex = message.indexOf(SEPARATOR);
          String connectionId = message.substring(0, separatorIndex);
          byte[] payload = HexFormat.of().parseHex(message.substring(separatorIndex + 1));
          payloadSender.send(connectionId, payload);
        }
      } catch (RuntimeException exception) {
        if (running) {
          LOGGER.log(Level.WARNING, "Falha lendo a fila de respostas do POS", exception);
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
