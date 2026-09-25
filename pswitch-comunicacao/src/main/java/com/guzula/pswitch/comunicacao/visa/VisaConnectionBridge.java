package com.guzula.pswitch.comunicacao.visa;

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
 * Ponte entre a conexão de verdade com a Visa e as filas do Redis — só existe no processo de
 * comunicação, nunca no switch.
 *
 * <p>Diferente do HSM, a Visa não tem um header de posição fixa que sirva de endereço de resposta
 * de forma simples de extrair sem entender o protocolo ISO 8583 inteiro — então essa ponte é
 * totalmente burra nos dois sentidos: só repassa bytes crus. Quem correlaciona (por terminalId+NSU,
 * via {@code RedisPendingCorrelator}) é o próprio switch (VisaService, em pswitch-brand-visa),
 * lendo continuamente a fila de respostas.
 *
 * <ul>
 *   <li>switch → Visa: consome {@code visa:pedidos} e manda o payload pela conexão real.
 *   <li>Visa → switch: recebe a resposta pela conexão real ({@link #handleInbound}) e publica, sem
 *       processar, em {@code visa:respostas}.
 * </ul>
 */
@Component
public final class VisaConnectionBridge implements InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(VisaConnectionBridge.class.getName());
  private static final String CONNECTION_NAME = "VISA";
  // Precisam bater com as constantes homônimas em VisaService (pswitch-brand-visa) — não dá pra
  // importar de lá porque pswitch-comunicacao não depende de pswitch-brand-visa (evita ciclo).
  private static final String REQUEST_QUEUE_KEY = "visa:pedidos";
  private static final String RESPONSE_QUEUE_KEY = "visa:respostas";
  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

  private final OutboundPayloadSender payloadSender;
  private final StringRedisTemplate redisTemplate;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean running = true;

  public VisaConnectionBridge(
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
    redisTemplate.opsForList().rightPush(RESPONSE_QUEUE_KEY, HexFormat.of().formatHex(payload));
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
          LOGGER.log(Level.WARNING, "Falha lendo a fila de pedidos da Visa", exception);
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
