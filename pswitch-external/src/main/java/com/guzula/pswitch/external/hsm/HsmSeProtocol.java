package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Monta o comando SE e interpreta sua resposta SF. */
@Component
public class HsmSeProtocol {

  private static final String SE_PREFIX = "SE01";
  private static final String KSN_DESCRIPTOR = "505";
  private static final String CARD_DATA_TYPE = "3";
  private static final String SE_FOOTER = ";99%01";
  private static final String SF_COMMAND = "SF";
  private static final String SUCCESS_CODE = "00";

  private static final int COMMAND_OFFSET = 4;
  private static final int COMMAND_LENGTH = 2;
  private static final int RESPONSE_CODE_OFFSET = 6;
  private static final int RESPONSE_CODE_LENGTH = 2;
  private static final int TRACK_LENGTH_OFFSET = 8;
  private static final int TRACK_LENGTH_SIZE = 5;
  private static final int TRACK_OFFSET = 13;

  public byte[] buildSeRequest(CanonicalTransaction canonical, String sourceKey) {
    validateSourceKey(sourceKey);
    String ksn = ksn(canonical);
    byte[] encryptedCardData = encryptedCardData(canonical);
    String dataLength = "%05d".formatted(encryptedCardData.length);

    byte[] prefix =
        (SE_PREFIX + sourceKey + KSN_DESCRIPTOR + ksn + CARD_DATA_TYPE + dataLength)
            .getBytes(StandardCharsets.US_ASCII);

    return concatenate(prefix, encryptedCardData, SE_FOOTER.getBytes(StandardCharsets.US_ASCII));
  }

  public SeResult parseSfResponse(byte[] response) {
    if (response.length < TRACK_OFFSET) {
      throw new IllegalStateException("Resposta SF menor que 13 bytes");
    }

    String command = ascii(response, COMMAND_OFFSET, COMMAND_LENGTH);
    if (!SF_COMMAND.equals(command)) {
      throw new IllegalStateException("Comando de resposta HSM inválido: " + command);
    }

    String responseCode = ascii(response, RESPONSE_CODE_OFFSET, RESPONSE_CODE_LENGTH);
    String trackLengthText = ascii(response, TRACK_LENGTH_OFFSET, TRACK_LENGTH_SIZE);
    if (!trackLengthText.matches("\\d{5}")) {
      throw new IllegalStateException("Tamanho de trilha inválido na resposta SF");
    }

    int trackLength = Integer.parseInt(trackLengthText);
    validateResponseLength(response, trackLength);
    if (!SUCCESS_CODE.equals(responseCode)) {
      throw new IllegalStateException("HSM rejeitou o comando SE: código=" + responseCode);
    }

    return parseTrack(ascii(response, TRACK_OFFSET, trackLength));
  }

  private SeResult parseTrack(String track) {
    int separator = track.indexOf('=');
    if (separator <= 0 || track.length() < separator + 8) {
      throw new IllegalStateException("Trilha retornada pelo HSM possui formato inválido");
    }

    return new SeResult(
        track,
        track.substring(0, separator),
        track.substring(separator + 1, separator + 5),
        track.substring(separator + 5, separator + 8));
  }

  private void validateSourceKey(String sourceKey) {
    if (sourceKey == null || sourceKey.isBlank()) {
      throw new IllegalStateException("Chave BDK de origem ausente para o comando SE");
    }
    if (!sourceKey.matches("[0-9A-Fa-f]{32}")) {
      throw new IllegalStateException("Chave BDK deve possuir 32 caracteres hexadecimais");
    }
  }

  private void validateResponseLength(byte[] response, int trackLength) {
    if (response.length != TRACK_OFFSET + trackLength) {
      throw new IllegalStateException(
          "Resposta SF com tamanho incompatível: declarado="
              + trackLength
              + ", recebido="
              + (response.length - TRACK_OFFSET));
    }
  }

  private String ksn(CanonicalTransaction canonical) {
    if (canonical.getSecurity() == null || canonical.getSecurity().getKsn() == null) {
      throw new IllegalStateException("KSN ausente para o comando SE");
    }
    CanonicalTransaction.Security.Ksn ksn = canonical.getSecurity().getKsn();
    String value =
        required(ksn.getBdkIndicator(), "indicador BDK")
            + required(ksn.getPinPadIndicator(), "indicador do pinpad")
            + required(ksn.getTransactionCounter(), "contador da transação");
    if (!value.matches("[0-9A-Fa-f]{20}")) {
      throw new IllegalStateException("KSN deve possuir 20 caracteres hexadecimais");
    }
    return value;
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

  private String ascii(byte[] payload, int offset, int length) {
    return new String(payload, offset, length, StandardCharsets.US_ASCII);
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

  public record SeResult(
      String track, String cardNumber, String expirationDate, String serviceCode) {}
}
