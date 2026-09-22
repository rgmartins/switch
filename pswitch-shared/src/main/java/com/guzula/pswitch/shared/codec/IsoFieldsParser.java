package com.guzula.pswitch.shared.codec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Abre os campos presentes no bitmap usando um schema declarativo. */
public final class IsoFieldsParser {

  private final Map<Integer, FieldDef> schema;
  private final Map<Integer, FieldValueDecoder> customDecoders;

  public IsoFieldsParser(Map<Integer, FieldDef> schema) {
    this(schema, Map.of());
  }

  public IsoFieldsParser(
      Map<Integer, FieldDef> schema, Map<Integer, FieldValueDecoder> customDecoders) {
    this.schema = Map.copyOf(schema);
    this.customDecoders = Map.copyOf(customDecoders);
  }

  public Map<Integer, ParsedField> parse(ByteCursor cursor, IsoBitmap bitmap) {
    Map<Integer, ParsedField> fields = new LinkedHashMap<>();

    for (int de = 2; de <= bitmap.bitCount(); de++) {
      if (!bitmap.isSet(de)) {
        continue;
      }

      FieldDef definition = schema.get(de);
      if (definition == null) {
        throw new IsoParseException("DE %d está ativo, mas não existe no schema".formatted(de));
      }

      int length = readLength(cursor, definition);
      int dataOffset = cursor.position();
      byte[] raw = cursor.readBytes(length, "DE %03d (%s)".formatted(de, definition.key()));
      FieldValueDecoder decoder =
          customDecoders.getOrDefault(
              de, (value, field) -> IsoCodec.parseIsoField(value, field.type(), field.length()));
      Object value = decoder.decode(raw, definition);
      fields.put(de, new ParsedField(definition, dataOffset, length, raw, value));
    }

    return Collections.unmodifiableMap(fields);
  }

  private int readLength(ByteCursor cursor, FieldDef definition) {
    if (definition.format() == LengthFormat.FIXED) {
      return definition.length();
    }

    byte[] prefix =
        cursor.readBytes(
            definition.format().prefixBytes(), "tamanho do DE %03d".formatted(definition.de()));
    return BcdCodec.decodeLength(prefix, "DE %03d".formatted(definition.de()));
  }
}
