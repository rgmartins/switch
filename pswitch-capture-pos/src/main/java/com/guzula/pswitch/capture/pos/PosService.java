package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosParserService;
import com.guzula.pswitch.nucleo.ChannelResponder;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Referência: pos.service.ts (guzula-switch). Recebe um payload completo da camada de composição,
 * aciona ComumService -> NucleoService, e via {@link #sendResponse} devolve a resposta ao terminal
 * (PosPackerService).
 *
 * <p>Não fala com socket algum — consome {@code pos:pedidos} (numa thread própria) e publica em
 * {@code pos:respostas}. Quem segura as conexões de verdade com os terminais é o {@code
 * PosConnectionBridge}, em pswitch-comunicacao, processo separado. Cada mensagem nas filas carrega
 * {@code connectionId + "|" + payload em hex} — o POS tem muitas conexões simultâneas, então
 * precisa desse endereço junto (diferente de HSM/Visa, uma conexão só).
 *
 * <p>TODO: portar o fluxo completo (ver diagrama em guzula-switch/CLAUDE.md). Por enquanto {@link
 * #sendResponse} apenas ecoa de volta o payload bruto recebido, até {@link PosPackerService}
 * empacotar a resposta real a partir de {@code canonical.response}.
 */
@Service
public class PosService implements ChannelResponder {

  private static final Logger LOGGER = Logger.getLogger(PosService.class.getName());
  private static final String POS = "POS";
  private static final String SEPARATOR = "|";
  private static final String REQUEST_QUEUE_KEY = "pos:pedidos";
  private static final String RESPONSE_QUEUE_KEY = "pos:respostas";
  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

  private final PosParserService parser;
  private final PosMapperService mapper;
  private final NucleoService nucleoService;
  private final StringRedisTemplate redisTemplate;
  private final Map<String, byte[]> pendingRequestByConnection = new ConcurrentHashMap<>();
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean running = true;

  public PosService(
      PosParserService parser,
      PosMapperService mapper,
      NucleoService nucleoService,
      StringRedisTemplate redisTemplate) {
    this.parser = parser;
    this.mapper = mapper;
    this.nucleoService = nucleoService;
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  public void start() {
    worker.execute(this::consumeRequestsLoop);
  }

  @PreDestroy
  public void stop() {
    running = false;
    worker.shutdownNow();
  }

  @Override
  public String channelName() {
    return POS;
  }

  @Override
  public void sendResponse(CanonicalTransaction transaction) {
    String connectionId = transaction.getCommunication().getSocketId();
    byte[] payload = pendingRequestByConnection.remove(connectionId);
    logCanonical(transaction);
    // TODO: portar pos.service.ts#sendResponseToPOS — empacotar transaction.response via
    // PosPackerService em vez de ecoar o payload bruto recebido.
    String message = connectionId + SEPARATOR + HexFormat.of().formatHex(payload);
    redisTemplate.opsForList().rightPush(RESPONSE_QUEUE_KEY, message);
  }

  private void consumeRequestsLoop() {
    while (running) {
      try {
        String message = redisTemplate.opsForList().leftPop(REQUEST_QUEUE_KEY, POLL_TIMEOUT);
        if (message != null) {
          int separatorIndex = message.indexOf(SEPARATOR);
          String connectionId = message.substring(0, separatorIndex);
          byte[] payload = HexFormat.of().parseHex(message.substring(separatorIndex + 1));
          handleInbound(connectionId, payload);
        }
      } catch (RuntimeException exception) {
        if (running) {
          LOGGER.log(Level.WARNING, "Falha lendo a fila de pedidos do POS", exception);
        }
      }
    }
  }

  private void handleInbound(String connectionId, byte[] payload) {
    PosMessage message = parser.parse(payload);
    System.out.print(message.toMultilineString());

    CanonicalTransaction canonical = mapper.toCanonical(message, connectionId);
    pendingRequestByConnection.put(connectionId, payload);
    // processTransaction chama handleResponse -> sendResponse (deste próprio PosService) no final
    // — não devolvemos a resposta aqui, para manter o roteamento por canal centralizado no
    // NucleoService.
    nucleoService.processTransaction(canonical);
  }

  private void logCanonical(CanonicalTransaction transaction) {
    System.out.println("****************************************");
    System.out.println("* Canônico com dados do terminal new   *");
    System.out.println("****************************************");
    System.out.println(transaction);
    System.out.printf(
        "Canonical  terminalId=%s valorCentavos=%d moeda=%s%n",
        transaction.getTerminalId(),
        transaction.getOperation().getAmount(),
        transaction.getOperation().getCurrencyCode());
  }
}
