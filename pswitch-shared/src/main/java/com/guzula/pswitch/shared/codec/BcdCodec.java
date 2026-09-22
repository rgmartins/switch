package com.guzula.pswitch.shared.codec;

/** Conversões de números BCD compactados. */
public final class BcdCodec {

  private BcdCodec() {}

  public static String decode(byte[] bytes) {
    StringBuilder digits = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      int high = (value >>> 4) & 0x0F;
      int low = value & 0x0F;
      if (high > 9 || low > 9) {
        throw new IsoParseException("Valor BCD inválido: nibble maior que 9");
      }
      digits.append(high).append(low);
    }
    return digits.toString();
  }

  public static int decodeLength(byte[] bytes, String fieldName) {
    try {
      return Integer.parseInt(decode(bytes));
    } catch (NumberFormatException exception) {
      throw new IsoParseException("Tamanho BCD inválido em " + fieldName);
    }
  }
}
