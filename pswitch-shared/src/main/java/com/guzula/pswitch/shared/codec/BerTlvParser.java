package com.guzula.pswitch.shared.codec;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Leitor BER-TLV usado por campos EMV. Mantém os valores em hexadecimal. */
public final class BerTlvParser {

  private BerTlvParser() {}

  public static Map<String, String> parse(byte[] source) {
    Map<String, String> tags = new LinkedHashMap<>();
    ByteCursor cursor = new ByteCursor(source);

    while (cursor.hasRemaining()) {
      int first = Byte.toUnsignedInt(cursor.readBytes(1, "tag TLV")[0]);
      if (first == 0x00 || first == 0xFF) {
        continue;
      }

      StringBuilder tag = new StringBuilder("%02x".formatted(first));
      if ((first & 0x1F) == 0x1F) {
        int next;
        do {
          next = Byte.toUnsignedInt(cursor.readBytes(1, "continuação da tag TLV")[0]);
          tag.append("%02x".formatted(next));
        } while ((next & 0x80) != 0);
      }

      int firstLength = Byte.toUnsignedInt(cursor.readBytes(1, "tamanho TLV")[0]);
      int length = firstLength;
      if ((firstLength & 0x80) != 0) {
        int lengthBytes = firstLength & 0x7F;
        if (lengthBytes == 0 || lengthBytes > 4) {
          throw new IsoParseException("Formato de tamanho TLV não suportado");
        }
        length = 0;
        for (byte value : cursor.readBytes(lengthBytes, "tamanho estendido TLV")) {
          length = (length << 8) | Byte.toUnsignedInt(value);
        }
      }

      byte[] value = cursor.readBytes(length, "valor da tag " + tag);
      tags.put(tag.toString(), HexFormat.of().formatHex(value));
    }

    return Map.copyOf(tags);
  }
}
