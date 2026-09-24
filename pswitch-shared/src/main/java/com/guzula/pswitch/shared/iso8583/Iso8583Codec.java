package com.guzula.pswitch.shared.iso8583;

/**
 * Codec de campos ISO 8583 (BCD/EBCDIC), compartilhado por todas as bandeiras (Visa e, futuramente,
 * Mastercard). Subconjunto mínimo — só o necessário pra montar/ler o cabeçalho VXFS e os DEs
 * numéricos/alfanuméricos mais comuns. Referência: shared/utils/iso-codec.ts (guzula-switch).
 */
public final class Iso8583Codec {

  private Iso8583Codec() {}

  /** BCD a partir de uma string de dígitos — completa com '0' à esquerda se for ímpar. */
  public static byte[] encodeBCD(String digits) {
    String clean = digits.length() % 2 == 0 ? digits : "0" + digits;
    byte[] out = new byte[clean.length() / 2];
    for (int i = 0; i < out.length; i++) {
      int high = Character.digit(clean.charAt(i * 2), 10);
      int low = Character.digit(clean.charAt(i * 2 + 1), 10);
      out[i] = (byte) ((high << 4) | low);
    }
    return out;
  }

  /** BCD de um número, preenchido com zeros à esquerda até {@code lengthBytes} bytes. */
  public static byte[] encodeBCD(long value, int lengthBytes) {
    String digits = Long.toString(value);
    int digitsCapacity = lengthBytes * 2;
    if (digits.length() > digitsCapacity) {
      throw new IllegalArgumentException(
          "Valor %d não cabe em %d bytes BCD".formatted(value, lengthBytes));
    }
    return encodeBCD("0".repeat(digitsCapacity - digits.length()) + digits);
  }

  public static String decodeBCD(byte[] bytes) {
    StringBuilder out = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      out.append((b >> 4) & 0x0f).append(b & 0x0f);
    }
    return out.toString();
  }

  /** EBCDIC alfanumérico, completado com espaços à direita até {@code length}. */
  public static byte[] encodeEBCDIC(String value, int length) {
    String padded = pad(value, length, ' ');
    byte[] out = new byte[padded.length()];
    for (int i = 0; i < padded.length(); i++) {
      out[i] = asciiToEbcdic(padded.charAt(i));
    }
    return out;
  }

  /** EBCDIC numérico, completado com zeros à esquerda até {@code length}. */
  public static byte[] encodeEBCDICNumeric(long value, int length) {
    String digits = Long.toString(value);
    if (digits.length() > length) {
      throw new IllegalArgumentException(
          "Valor %d não cabe em %d posições EBCDIC".formatted(value, length));
    }
    return encodeEBCDIC("0".repeat(length - digits.length()) + digits, length);
  }

  public static String decodeEBCDIC(byte[] bytes) {
    StringBuilder out = new StringBuilder(bytes.length);
    for (byte b : bytes) {
      out.append(ebcdicToAscii(b));
    }
    return out.toString().trim();
  }

  private static String pad(String value, int length, char padChar) {
    if (value.length() >= length) return value.substring(0, length);
    return value + String.valueOf(padChar).repeat(length - value.length());
  }

  private static byte asciiToEbcdic(char c) {
    if (c >= '0' && c <= '9') return (byte) (c - '0' + 0xf0);
    if (c >= 'A' && c <= 'I') return (byte) (c - 'A' + 0xc1);
    if (c >= 'J' && c <= 'R') return (byte) (c - 'J' + 0xd1);
    if (c >= 'S' && c <= 'Z') return (byte) (c - 'S' + 0xe2);
    if (c == ' ') return (byte) 0x40;
    return (byte) 0x6f; // '?'
  }

  private static char ebcdicToAscii(byte raw) {
    int b = raw & 0xff;
    if (b >= 0xf0 && b <= 0xf9) return (char) (b - 0xf0 + '0');
    if (b >= 0xc1 && b <= 0xc9) return (char) (b - 0xc1 + 'A');
    if (b >= 0xd1 && b <= 0xd9) return (char) (b - 0xd1 + 'J');
    if (b >= 0xe2 && b <= 0xe9) return (char) (b - 0xe2 + 'S');
    if (b == 0x40) return ' ';
    return '?';
  }
}
