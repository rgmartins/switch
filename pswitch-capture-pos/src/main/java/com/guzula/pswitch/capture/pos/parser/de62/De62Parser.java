package com.guzula.pswitch.capture.pos.parser.de62;

import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.BcdSubfield;
import com.guzula.pswitch.shared.codec.BcdSubfieldReader;
import com.guzula.pswitch.shared.codec.BerTlvParser;
import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.FieldValueDecoder;
import com.guzula.pswitch.shared.codec.IsoParseException;
import com.guzula.pswitch.shared.util.PrettyJson;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Abre os subcampos do DE 62 (dados privados da rede). */
public final class De62Parser implements FieldValueDecoder {

  @Override
  public Object decode(byte[] raw, FieldDef definition) {
    Map<String, Subfield> subfields = new LinkedHashMap<>();
    for (BcdSubfield subfield : BcdSubfieldReader.readAll(raw, "DE062")) {
      subfields.put(subfield.id(), parseSubfield(subfield));
    }
    return new Data(HexFormat.of().formatHex(raw), Collections.unmodifiableMap(subfields));
  }

  private Subfield parseSubfield(BcdSubfield subfield) {
    if (subfield.id().equals("05")) {
      return new Subfield(
          "05", "Confirmacao dos Dados EMV - POS", parseEmvConfirmation(subfield.body()));
    }
    return new Subfield(
        subfield.id(), "Subcampo desconhecido", HexFormat.of().formatHex(subfield.body()));
  }

  private EmvConfirmation parseEmvConfirmation(byte[] body) {
    if (body.length < 14) {
      throw new IsoParseException(
          "DE062.05 incompleto: esperados ao menos 14 bytes, recebidos %d".formatted(body.length));
    }

    int emvLength = BcdCodec.decodeLength(slice(body, 12, 2), "DE062.05.dadosEmv");
    int emvEnd = 14 + emvLength;
    if (emvEnd > body.length) {
      throw new IsoParseException(
          "DE062.05 declara %d bytes EMV, mas possui apenas %d"
              .formatted(emvLength, body.length - 14));
    }

    byte[] emv = slice(body, 14, emvLength);
    byte[] additionalData = slice(body, emvEnd, body.length - emvEnd);
    return new EmvConfirmation(
        BcdCodec.decode(slice(body, 0, 3)),
        BcdCodec.decode(slice(body, 3, 6)),
        new String(body, 9, 3, StandardCharsets.US_ASCII).trim(),
        emvLength,
        HexFormat.of().formatHex(emv),
        BerTlvParser.parse(emv),
        HexFormat.of().formatHex(additionalData));
  }

  private byte[] slice(byte[] source, int offset, int length) {
    byte[] result = new byte[length];
    System.arraycopy(source, offset, result, 0, length);
    return result;
  }

  public record Data(String rawHex, Map<String, Subfield> subfields) {

    @Override
    public String toString() {
      return PrettyJson.format(subfields);
    }
  }

  public record Subfield(String id, String description, Object details) {}

  public record EmvConfirmation(
      String documentNumber,
      String transactionDateTime,
      String responseCode,
      int emvLength,
      String emvHex,
      Map<String, String> emvTags,
      String additionalDataHex) {}
}
