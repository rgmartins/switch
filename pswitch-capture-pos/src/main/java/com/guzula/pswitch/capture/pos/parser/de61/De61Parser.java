package com.guzula.pswitch.capture.pos.parser.de61;

import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.BcdSubfield;
import com.guzula.pswitch.shared.codec.BcdSubfieldReader;
import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.FieldValueDecoder;
import com.guzula.pswitch.shared.codec.IsoParseException;
import com.guzula.pswitch.shared.util.PrettyJson;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Abre os subcampos do DE 61 (dados privados do POS). */
public final class De61Parser implements FieldValueDecoder {

  @Override
  public Object decode(byte[] raw, FieldDef definition) {
    Map<String, Subfield> subfields = new LinkedHashMap<>();
    for (BcdSubfield subfield : BcdSubfieldReader.readAll(raw, "DE061")) {
      subfields.put(subfield.id(), parseSubfield(subfield));
    }
    return new Data(HexFormat.of().formatHex(raw), Collections.unmodifiableMap(subfields));
  }

  private Subfield parseSubfield(BcdSubfield subfield) {
    return switch (subfield.id()) {
      case "01" -> new Subfield("01", "Identificacao do Software", parseSoftware(subfield.body()));
      case "02" ->
          new Subfield("02", "Numero de Serie do POS", parsePosSerialNumber(subfield.body()));
      case "08" ->
          new Subfield("08", "Financiamento e Pre-datamento", parseFinancing(subfield.body()));
      case "14" ->
          new Subfield("14", "Codigo do Produto Matriz", parseMatrixProduct(subfield.body()));
      case "16" ->
          new Subfield(
              "16", "Identificacao Positiva", parsePositiveIdentification(subfield.body()));
      case "28" ->
          new Subfield(
              "28", "Codigo do Produto Secundario", parseSecondaryProduct(subfield.body()));
      default ->
          new Subfield(
              subfield.id(), "Subcampo desconhecido", HexFormat.of().formatHex(subfield.body()));
    };
  }

  private SoftwareIdentification parseSoftware(byte[] body) {
    requireLength(body, 12, "DE061.01");
    return new SoftwareIdentification(ascii(body, 0, 12));
  }

  private PosSerialNumber parsePosSerialNumber(byte[] body) {
    requireLength(body, 1, "DE061.02");
    int serialLength = BcdCodec.decodeLength(slice(body, 0, 1), "DE061.02.numeroSerie");
    requireLength(body, serialLength + 1, "DE061.02");
    return new PosSerialNumber(serialLength, ascii(body, 1, serialLength));
  }

  private FinancingData parseFinancing(byte[] body) {
    requireLength(body, 6, "DE061.08");
    String financingCode = bcd(body, 0, 1);
    String financingDescription =
        switch (financingCode) {
          case "01" -> "Venda Financiada ADM";
          case "02" -> "Venda Financiada Loja";
          case "03" -> "Crediario Debito";
          case "04" -> "Venda Pre-datada";
          case "05" -> "Crediario Credito";
          case "06" -> "Parcelado ADM pos-datado";
          default -> "Desconhecido";
        };
    return new FinancingData(
        new FinancingType(financingCode, financingDescription),
        BcdCodec.decodeLength(slice(body, 1, 1), "DE061.08.quantidadeParcelas"),
        bcd(body, 2, 3),
        bcd(body, 5, 1));
  }

  private MatrixProduct parseMatrixProduct(byte[] body) {
    requireLength(body, 2, "DE061.14");
    return new MatrixProduct(bcd(body, 0, 2));
  }

  private PositiveIdentification parsePositiveIdentification(byte[] body) {
    requireLength(body, 10, "DE061.16");
    int responseLength = BcdCodec.decodeLength(slice(body, 9, 1), "DE061.16.resposta");
    requireLength(body, responseLength + 10, "DE061.16");
    return new PositiveIdentification(
        bcd(body, 0, 1),
        HexFormat.of().formatHex(slice(body, 1, 8)),
        responseLength,
        ascii(body, 10, responseLength));
  }

  private SecondaryProduct parseSecondaryProduct(byte[] body) {
    requireLength(body, 2, "DE061.28");
    return new SecondaryProduct(bcd(body, 0, 2));
  }

  private void requireLength(byte[] body, int minimum, String fieldName) {
    if (body.length < minimum) {
      throw new IsoParseException(
          "%s incompleto: esperados ao menos %d bytes, recebidos %d"
              .formatted(fieldName, minimum, body.length));
    }
  }

  private String bcd(byte[] source, int offset, int length) {
    return BcdCodec.decode(slice(source, offset, length));
  }

  private String ascii(byte[] source, int offset, int length) {
    return new String(source, offset, length, StandardCharsets.US_ASCII);
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

  public record SoftwareIdentification(String softwareId) {}

  public record PosSerialNumber(int length, String serialNumber) {}

  public record FinancingData(
      FinancingType financingType,
      int installmentCount,
      String preDatedDate,
      String numberOfDays) {}

  public record FinancingType(String code, String description) {}

  public record MatrixProduct(String productCode) {}

  public record PositiveIdentification(
      String questionCode, String nextFieldMask, int responseLength, String response) {}

  public record SecondaryProduct(String productCode) {}
}
