package com.guzula.pswitch.brand.visa;

import com.guzula.pswitch.brand.visa.packer.VisaPackerService;
import com.guzula.pswitch.brand.visa.parser.VisaAuthorizationResponse;
import com.guzula.pswitch.brand.visa.parser.VisaParserService;
import com.guzula.pswitch.comum.tableresponse.TableResponseService;
import com.guzula.pswitch.nucleo.BrandHandler;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.nucleo.correlation.RedisPendingCorrelator;
import com.guzula.pswitch.shared.SwitchConstants;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.rules.Obs;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Referência: visa.service.ts (guzula-switch). Correlaciona request/response outbound por
 * terminalId+NSU usando {@link RedisPendingCorrelator}, para que a resposta possa ser processada
 * por uma réplica diferente da que enviou o pedido. Ver docs/arquitetura/topologia-implantacao.md.
 *
 * <p>Não fala com socket algum — publica o pedido em {@code visa:pedidos} e lê as respostas de
 * {@code visa:respostas} numa thread própria (diferente do HSM, que bloqueia esperando; aqui é
 * fogo-e-esquece, então precisa de um consumidor contínuo). Quem segura a conexão de verdade é o
 * {@code VisaConnectionBridge}, em pswitch-comunicacao, processo separado. Essa classe não fica
 * sabendo de nenhum dos dois — mora aqui de propósito, pra o switch não depender de
 * pswitch-comunicacao.
 *
 * <p>{@link #sweepExpiredAuthorizations} varre periodicamente as transações nunca respondidas
 * dentro de {@code responseTimeout} e as converte em negação — sem isso, uma bandeira que nunca
 * responde deixaria a transação (e o POS) esperando pra sempre. Referência: handleTimeout
 * (visa.service.ts).
 */
@Service
public class VisaService implements BrandHandler {

  private static final Logger LOGGER = Logger.getLogger(VisaService.class.getName());
  private static final String APPROVED_RESPONSE_CODE = "00";
  private static final String PENDING_KEY_PREFIX = "visa:pendente:";
  private static final Duration PENDING_TTL_BUFFER = Duration.ofSeconds(30);
  private static final String REQUEST_QUEUE_KEY = "visa:pedidos";
  private static final String RESPONSE_QUEUE_KEY = "visa:respostas";
  private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

  private final VisaPackerService packerService;
  private final VisaParserService parserService;
  private final NucleoService nucleoService;
  private final TableResponseService tableResponseService;
  private final RedisPendingCorrelator<CanonicalTransaction> correlator;
  private final StringRedisTemplate redisTemplate;
  private final Duration responseTimeout;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private volatile boolean running = true;

  public VisaService(
      VisaPackerService packerService,
      VisaParserService parserService,
      NucleoService nucleoService,
      TableResponseService tableResponseService,
      StringRedisTemplate redisTemplate,
      @Value("${pswitch.visa.response-timeout:10s}") Duration responseTimeout) {
    this.packerService = packerService;
    this.parserService = parserService;
    this.nucleoService = nucleoService;
    this.tableResponseService = tableResponseService;
    this.correlator =
        new RedisPendingCorrelator<>(redisTemplate, PENDING_KEY_PREFIX, CanonicalTransaction.class);
    this.redisTemplate = redisTemplate;
    this.responseTimeout = responseTimeout;
  }

  @PostConstruct
  void start() {
    worker.execute(this::consumeResponsesLoop);
  }

  @PreDestroy
  void stop() {
    running = false;
    worker.shutdownNow();
  }

  @Override
  public String brand() {
    return String.valueOf(SwitchConstants.Brand.VISA);
  }

  @Override
  public void authorize(CanonicalTransaction transaction) {
    String key = correlationKey(transaction.getTerminalId(), transaction.getNsu());
    correlator.store(key, transaction, responseTimeout.plus(PENDING_TTL_BUFFER));
    byte[] request = packerService.pack(transaction);
    redisTemplate.opsForList().rightPush(REQUEST_QUEUE_KEY, HexFormat.of().formatHex(request));
    LOGGER.info(
        () ->
            "[VisaService] Pedido publicado na fila da Visa (%d bytes): %s"
                .formatted(request.length, bytesToHex(request)));
  }

  private void consumeResponsesLoop() {
    while (running) {
      try {
        String hexResponse = redisTemplate.opsForList().leftPop(RESPONSE_QUEUE_KEY, POLL_TIMEOUT);
        if (hexResponse != null) {
          handleInbound(HexFormat.of().parseHex(hexResponse));
        }
      } catch (RuntimeException exception) {
        if (running) {
          LOGGER.log(Level.WARNING, "Falha lendo a fila de respostas da Visa", exception);
        }
      }
    }
  }

  private void handleInbound(byte[] payload) {
    VisaAuthorizationResponse response = parserService.parse(payload);
    String key = correlationKey(response.terminalId(), String.valueOf(response.nsu()));
    CanonicalTransaction transaction = correlator.claim(key);
    if (transaction == null) {
      LOGGER.warning(
          () ->
              "[VisaService] Resposta sem transação correspondente (chave %s): %s"
                  .formatted(key, bytesToHex(payload)));
      return;
    }

    populateResponse(transaction, response);
    LOGGER.info(
        () ->
            "[VisaService] Resposta da Visa recebida: código=%s, autorização=%s, chave=%s"
                .formatted(response.responseCode(), response.authorizationCode(), key));
    nucleoService.handleResponse(transaction);
  }

  /**
   * Converte em negação (timeout) qualquer autorização parada há mais de {@code responseTimeout}.
   */
  @Scheduled(fixedDelayString = "${pswitch.visa.timeout-sweep-interval:1s}")
  void sweepExpiredAuthorizations() {
    correlator.sweepExpired(responseTimeout, this::timeout);
  }

  private void timeout(String key, CanonicalTransaction transaction) {
    tableResponseService.populateResponseFromRule(transaction, Obs.RULE_091_BRAND_TIMEOUT);
    LOGGER.warning(
        () -> "[VisaService] Timeout aguardando resposta da Visa (chave %s)".formatted(key));
    nucleoService.handleResponse(transaction);
  }

  private void populateResponse(
      CanonicalTransaction transaction, VisaAuthorizationResponse visaResponse) {
    CanonicalTransaction.Response response = new CanonicalTransaction.Response();
    response.setWhoResponded(SwitchConstants.WhoResponded.BRAND_VISA);
    response.setResponseCode(visaResponse.responseCode());
    response.setIssuerResponseCode(visaResponse.responseCode());
    response.setAuthorizationCode(visaResponse.authorizationCode());
    response.setMessage(
        APPROVED_RESPONSE_CODE.equals(visaResponse.responseCode()) ? "APROVADA" : "NAO AUTORIZADA");
    response.setMessageSource(SwitchConstants.MessageDescriptionSource.BRAND);
    response.setPaymentAccountReference("");
    CanonicalTransaction.Emv emv = new CanonicalTransaction.Emv();
    response.setEmv(emv);
    transaction.setResponse(response);
  }

  private static String correlationKey(String terminalId, String nsu) {
    return terminalId + nsu;
  }

  private static String bytesToHex(byte[] payload) {
    StringBuilder hex = new StringBuilder(payload.length * 2);
    for (byte b : payload) {
      hex.append(String.format("%02X", b));
    }
    return hex.toString();
  }
}
