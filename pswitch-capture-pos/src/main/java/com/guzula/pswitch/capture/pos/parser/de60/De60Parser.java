package com.guzula.pswitch.capture.pos.parser.de60;

import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.BcdSubfield;
import com.guzula.pswitch.shared.codec.BcdSubfieldReader;
import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.FieldValueDecoder;
import com.guzula.pswitch.shared.codec.IsoParseException;
import com.guzula.pswitch.shared.util.PrettyJson;

import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Abre os subcampos do DE 60 (informações adicionais do POS). */
public final class De60Parser implements FieldValueDecoder {

    @Override
    public Object decode(byte[] raw, FieldDef definition) {
        Map<String, Subfield> subfields = new LinkedHashMap<>();
        for (BcdSubfield subfield : BcdSubfieldReader.readAll(raw, "DE060")) {
            subfields.put(subfield.id(), parseSubfield(subfield));
        }
        return new Data(
                HexFormat.of().formatHex(raw),
                Collections.unmodifiableMap(subfields));
    }

    private Subfield parseSubfield(BcdSubfield subfield) {
        if (subfield.id().equals("12")) {
            return new Subfield(
                    "12",
                    "Dados de Criptografia da Trilha",
                    parseTrackEncryption(subfield.body()));
        }
        return new Subfield(
                subfield.id(),
                "Subcampo desconhecido",
                HexFormat.of().formatHex(subfield.body()));
    }

    private TrackEncryptionData parseTrackEncryption(byte[] body) {
        if (body.length < 14) {
            throw new IsoParseException(
                    "DE060.12 incompleto: esperados 14 bytes, recebidos %d"
                            .formatted(body.length));
        }

        String encryptionCode = BcdCodec.decode(new byte[]{body[3]});
        String encryptionDescription = switch (encryptionCode) {
            case "01" -> "DUKPT";
            case "03" -> "DUKPT Triple DES";
            default -> "Desconhecido";
        };

        byte[] ksn = new byte[10];
        System.arraycopy(body, 4, ksn, 0, ksn.length);
        return new TrackEncryptionData(
                BcdCodec.decode(new byte[]{body[0], body[1], body[2]}),
                new EncryptionType(encryptionCode, encryptionDescription),
                HexFormat.of().formatHex(ksn));
    }

    public record Data(String rawHex, Map<String, Subfield> subfields) {

        @Override
        public String toString() {
            return PrettyJson.format(subfields);
        }
    }

    public record Subfield(String id, String description, Object details) {
    }

    public record TrackEncryptionData(
            String cardBin,
            EncryptionType encryptionType,
            String ksn) {
    }

    public record EncryptionType(String code, String description) {
    }
}
