package com.guzula.pswitch.external.hsm;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/** Monta o comando G0 e interpreta sua resposta G1. */
@Component
public class HsmG0Protocol {

  private static final String G0_COMMAND = "G0";
  private static final String G1_COMMAND = "G1";
  private static final String KEY_MODE_AND_DERIVATION = "A05";
  private static final String SOURCE_AND_DESTINATION_FORMAT = "0101";
  private static final String G0_FOOTER = "%01";

  private static final int COMMAND_OFFSET = 4;
  private static final int COMMAND_LENGTH = 2;
  private static final int PIN_LENGTH_OFFSET = 6;
  private static final int PIN_LENGTH_SIZE = 4;
  private static final int PIN_BLOCK_OFFSET = 10;

  public byte[] buildG0Request(
      CanonicalTransaction canonical, String sourceKey, String destinationKey) {
    validateKey(sourceKey, "origem");
    validateKey(destinationKey, "destino");

    String command =
        G0_COMMAND
            + sourceKey
            + destinationKey
            + KEY_MODE_AND_DERIVATION
            + ksn(canonical)
            + pinBlock(canonical)
            + SOURCE_AND_DESTINATION_FORMAT
            + pan12(canonical)
            + G0_FOOTER;
    return command.getBytes(StandardCharsets.US_ASCII);
  }

  public G1Result parseG1Response(byte[] response) {
    if (response.length < PIN_BLOCK_OFFSET) {
      throw new IllegalStateException("Resposta G1 menor que 10 bytes");
    }

    String command = ascii(response, COMMAND_OFFSET, COMMAND_LENGTH);
    if (!G1_COMMAND.equals(command)) {
      throw new IllegalStateException("Comando de resposta HSM inválido: " + command);
    }

    String pinLengthText = ascii(response, PIN_LENGTH_OFFSET, PIN_LENGTH_SIZE);
    if (!pinLengthText.matches("\\d{4}")) {
      throw new IllegalStateException("Tamanho do PIN block inválido na resposta G1");
    }

    int pinLength = Integer.parseInt(pinLengthText);
    if (response.length != PIN_BLOCK_OFFSET + pinLength) {
      throw new IllegalStateException(
          "Resposta G1 com tamanho incompatível: declarado="
              + pinLength
              + ", recebido="
              + (response.length - PIN_BLOCK_OFFSET));
    }

    String translatedPinBlock = ascii(response, PIN_BLOCK_OFFSET, pinLength);
    if (!translatedPinBlock.matches("[0-9A-Fa-f]{16}")) {
      throw new IllegalStateException("PIN block inválido na resposta G1");
    }
    return new G1Result(translatedPinBlock);
  }

  private void validateKey(String key, String type) {
    if (key == null || !key.matches("[0-9A-Fa-f]{32}")) {
      throw new IllegalStateException(
          "Chave de " + type + " deve possuir 32 caracteres hexadecimais");
    }
  }

  private String ksn(CanonicalTransaction canonical) {
    if (canonical.getSecurity() == null || canonical.getSecurity().getKsn() == null) {
      throw new IllegalStateException("KSN ausente para o comando G0");
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

  private String pinBlock(CanonicalTransaction canonical) {
    if (canonical.getSecurity() == null) {
      throw new IllegalStateException("Dados de segurança ausentes para o comando G0");
    }

    String pinBlock = canonical.getSecurity().getPinBlock();
    if (pinBlock == null || !pinBlock.matches("[0-9A-Fa-f]{16}")) {
      throw new IllegalStateException("PIN block deve possuir 16 caracteres hexadecimais");
    }
    return pinBlock;
  }

  private String pan12(CanonicalTransaction canonical) {
    if (canonical.getCard() == null || canonical.getCard().getCardNumber() == null) {
      throw new IllegalStateException("Cartão ausente para o comando G0");
    }

    String pan = canonical.getCard().getCardNumber();
    if (!pan.matches("\\d{13,19}")) {
      throw new IllegalStateException("Cartão inválido para o comando G0");
    }
    return pan.substring(pan.length() - 13, pan.length() - 1);
  }

  private String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("KSN sem " + field + " para o comando G0");
    }
    return value;
  }

  private String ascii(byte[] payload, int offset, int length) {
    return new String(payload, offset, length, StandardCharsets.US_ASCII);
  }

  public record G1Result(String pinBlock) {}
}
