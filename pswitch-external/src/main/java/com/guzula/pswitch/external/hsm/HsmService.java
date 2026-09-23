package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.port.InboundPayloadHandler;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Executa comandos no HSM e correlaciona as respostas pelo header de quatro posições. */
@Service
public class HsmService implements InboundPayloadHandler {

  private static final Logger LOGGER = Logger.getLogger(HsmService.class.getName());
  private static final String HSM = "HSM";
  private static final String SE_FOOTER = ";99%01";
  private static final int MAX_HEADERS = 10_000;

  private final OutboundPayloadSender payloadSender;
  private final Duration responseTimeout;
  private final AtomicInteger nextHeader = new AtomicInteger();
  private final ConcurrentHashMap<String, CompletableFuture<byte[]>> pendingRequests =
      new ConcurrentHashMap<>();

  public HsmService(
      OutboundPayloadSender payloadSender,
      @Value("${pswitch.hsm.response-timeout:5s}") Duration responseTimeout) {
    if (responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()) {
      throw new IllegalArgumentException("Timeout de resposta do HSM deve ser maior que zero");
    }
    this.payloadSender = payloadSender;
    this.responseTimeout = responseTimeout;
  }

  @Override
  public String handlerName() {
    return HSM;
  }

  @Override
  public void handleInbound(String connectionId, byte[] payload) {
    if (payload.length < 4) {
      throw new IllegalArgumentException("Resposta HSM menor que o header de quatro posições");
    }

    String header = new String(payload, 0, 4, StandardCharsets.US_ASCII);
    CompletableFuture<byte[]> pending = pendingRequests.remove(header);
    if (pending == null) {
      LOGGER.warning(() -> "Resposta HSM sem requisição pendente: header=" + header);
      return;
    }
    pending.complete(payload.clone());
  }

  public void decryptCardData(CanonicalTransaction canonical, String sourceKey) {
    String ksn = ksn(canonical);
    byte[] encryptedCardData = encryptedCardData(canonical);
    if (sourceKey == null || sourceKey.isBlank()) {
      throw new IllegalStateException("Chave BDK de origem ausente para o comando SE");
    }

    PendingRequest pending = registerPendingRequest();
    try {
      byte[] request = buildSeRequest(pending.header(), sourceKey, ksn, encryptedCardData);
      payloadSender.send(HSM, request);
      LOGGER.info(() -> "Comando SE enviado ao HSM: header=" + pending.header());
      byte[] response = pending.future().get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
      parseSeResponse(response, canonical);
    } catch (TimeoutException exception) {
      throw new IllegalStateException(
          "Timeout aguardando resposta SF do HSM: header=" + pending.header(), exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Espera pela resposta SF foi interrompida", exception);
    } catch (ExecutionException exception) {
      throw new IllegalStateException("Falha aguardando resposta SF do HSM", exception.getCause());
    } finally {
      pendingRequests.remove(pending.header(), pending.future());
    }
  }

  private byte[] buildSeRequest(
      String header, String sourceKey, String ksn, byte[] encryptedCardData) {
    if (!sourceKey.matches("[0-9A-Fa-f]{32}")) {
      throw new IllegalStateException("Chave BDK deve possuir 32 caracteres hexadecimais");
    }
    if (!ksn.matches("[0-9A-Fa-f]{20}")) {
      throw new IllegalStateException("KSN deve possuir 20 caracteres hexadecimais");
    }

    String length = "%05d".formatted(encryptedCardData.length);
    return concatenate(
        (header + "SE01" + sourceKey + "505" + ksn + "3" + length)
            .getBytes(StandardCharsets.US_ASCII),
        encryptedCardData,
        SE_FOOTER.getBytes(StandardCharsets.US_ASCII));
  }

  private void parseSeResponse(byte[] response, CanonicalTransaction canonical) {
    if (response.length < 13) {
      throw new IllegalStateException("Resposta SF menor que 13 bytes");
    }

    String responseCommand = new String(response, 4, 2, StandardCharsets.US_ASCII);
    if (!"SF".equals(responseCommand)) {
      throw new IllegalStateException("Comando de resposta HSM inválido: " + responseCommand);
    }

    String responseCode = new String(response, 6, 2, StandardCharsets.US_ASCII);
    String trackLengthText = new String(response, 8, 5, StandardCharsets.US_ASCII);
    if (!trackLengthText.matches("\\d{5}")) {
      throw new IllegalStateException("Tamanho de trilha inválido na resposta SF");
    }

    int trackLength = Integer.parseInt(trackLengthText);
    if (response.length != 13 + trackLength) {
      throw new IllegalStateException(
          "Resposta SF com tamanho incompatível: declarado="
              + trackLength
              + ", recebido="
              + (response.length - 13));
    }
    if (!"00".equals(responseCode)) {
      throw new IllegalStateException("HSM rejeitou o comando SE: código=" + responseCode);
    }

    String track = new String(response, 13, trackLength, StandardCharsets.US_ASCII);
    populateCard(canonical, track);
    LOGGER.info("Resposta SF processada com sucesso");
  }

  private void populateCard(CanonicalTransaction canonical, String track) {
    int separator = track.indexOf('=');
    if (separator <= 0 || track.length() < separator + 8) {
      throw new IllegalStateException("Trilha retornada pelo HSM possui formato inválido");
    }

    CanonicalTransaction.Card card = canonical.getCard();
    if (card == null) {
      card = new CanonicalTransaction.Card();
      canonical.setCard(card);
    }
    card.setTrack(track);
    card.setCardNumber(track.substring(0, separator));
    card.setExpirationDate(track.substring(separator + 1, separator + 5));
    card.setServiceCode(track.substring(separator + 5, separator + 8));
  }

  private String ksn(CanonicalTransaction canonical) {
    if (canonical.getSecurity() == null || canonical.getSecurity().getKsn() == null) {
      throw new IllegalStateException("KSN ausente para o comando SE");
    }
    CanonicalTransaction.Security.Ksn ksn = canonical.getSecurity().getKsn();
    return required(ksn.getBdkIndicator(), "indicador BDK")
        + required(ksn.getPinPadIndicator(), "indicador do pinpad")
        + required(ksn.getTransactionCounter(), "contador da transação");
  }

  private byte[] encryptedCardData(CanonicalTransaction canonical) {
    if (canonical.getSecurity() == null) {
      throw new IllegalStateException("Dados de segurança ausentes para o comando SE");
    }
    String encrypted = canonical.getSecurity().getEncryptedCardData();
    if (encrypted == null || encrypted.isBlank()) {
      throw new IllegalStateException("Dados criptografados do cartão ausentes para o comando SE");
    }
    try {
      return HexFormat.of().parseHex(encrypted);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
          "Dados criptografados do cartão não são hexadecimais", exception);
    }
  }

  private String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("KSN sem " + field + " para o comando SE");
    }
    return value;
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

  private byte[] concatenate(byte[]... parts) {
    int totalLength = 0;
    for (byte[] part : parts) {
      totalLength += part.length;
    }
    byte[] result = new byte[totalLength];
    int offset = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, result, offset, part.length);
      offset += part.length;
    }
    return result;
  }

  private record PendingRequest(String header, CompletableFuture<byte[]> future) {}
}
