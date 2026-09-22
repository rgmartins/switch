package com.guzula.pswitch.capture.pos.parser.de55;

import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.BcdSubfield;
import com.guzula.pswitch.shared.codec.BcdSubfieldReader;
import com.guzula.pswitch.shared.codec.BerTlvParser;
import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.FieldValueDecoder;
import com.guzula.pswitch.shared.util.PrettyJson;

import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Abre os subcampos do DE 55, incluindo os dados EMV do subcampo 07. */
public final class De55Parser implements FieldValueDecoder {

    @Override
    public Object decode(byte[] raw, FieldDef definition) {
        Map<String, Subfield> subfields = new LinkedHashMap<>();
        for (BcdSubfield subfield : BcdSubfieldReader.readAll(raw, "DE055")) {
            subfields.put(subfield.id(), parseSubfield(subfield));
        }
        return new Data(
                HexFormat.of().formatHex(raw),
                Collections.unmodifiableMap(subfields));
    }

    private Subfield parseSubfield(BcdSubfield subfield) {
        return switch (subfield.id()) {
            case "01" -> new Subfield("01", "Consumo Moedeiro TIBC 1.0", parseTibc10(subfield.body()));
            case "02" -> new Subfield("02", "Consumo Moedeiro TIBC 3.0", parseTibc30(subfield.body()));
            case "07" -> new Subfield("07", "Dados EMV", new EmvData(BerTlvParser.parse(subfield.body())));
            default -> new Subfield(
                    subfield.id(),
                    "Subcampo desconhecido",
                    HexFormat.of().formatHex(subfield.body()));
        };
    }

    private Object parseTibc10(byte[] body) {
        if (body.length < 33) {
            return HexFormat.of().formatHex(body);
        }
        return new Tibc10Data(
                hex(body, 0, 8),
                hex(body, 8, 8),
                bcd(body, 16, 2),
                hex(body, 18, 3),
                bcd(body, 21, 3),
                hex(body, 24, 2),
                hex(body, 26, 1),
                hex(body, 27, 2),
                hex(body, 29, 4));
    }

    private Object parseTibc30(byte[] body) {
        if (body.length < 25) {
            return HexFormat.of().formatHex(body);
        }
        return new Tibc30Data(
                hex(body, 0, 8),
                hex(body, 8, 8),
                hex(body, 16, 2),
                bcd(body, 18, 3),
                hex(body, 21, 4));
    }

    private String bcd(byte[] source, int offset, int length) {
        return BcdCodec.decode(slice(source, offset, length));
    }

    private String hex(byte[] source, int offset, int length) {
        return HexFormat.of().formatHex(slice(source, offset, length));
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

    public record Subfield(String id, String description, Object details) {
    }

    public record EmvData(Map<String, String> tags) {
    }

    public record Tibc10Data(
            String certificate,
            String certificateVerificationData,
            String serviceCode,
            String cardTransactionCounter,
            String activationDate,
            String sourceFileIdentification,
            String accessConditions,
            String lastCardOperationData,
            String cardBalance) {
    }

    public record Tibc30Data(
            String certificate,
            String certificateVerificationData,
            String purseTransactionNumber,
            String activationDate,
            String cardBalance) {
    }
}
