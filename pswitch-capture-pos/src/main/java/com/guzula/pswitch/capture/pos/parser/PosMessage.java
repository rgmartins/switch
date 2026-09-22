package com.guzula.pswitch.capture.pos.parser;

import com.guzula.pswitch.shared.codec.IsoBitmap;
import com.guzula.pswitch.shared.codec.ParsedField;

import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resultado da abertura de uma mensagem POS. */
public record PosMessage(
        PosTpdu tpdu,
        String transparency,
        String mti,
        IsoBitmap bitmap,
        Map<Integer, ParsedField> fields,
        byte[] raw
) {

    public PosMessage {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        raw = raw.clone();
    }

    @Override
    public byte[] raw() {
        return raw.clone();
    }

    public String toMultilineString() {
        StringBuilder output = new StringBuilder();
        output.append("Mensagem POS aberta (%d bytes)%n".formatted(raw.length));
        output.append("TPDU   id=%s destino=%s origem=%s%n"
                .formatted(tpdu.id(), tpdu.destination(), tpdu.origin()));
        output.append("TRANS  %s%n".formatted(transparency));
        output.append("MTI    %s%n".formatted(mti));
        output.append("BITMAP %s%n".formatted(bitmap.hex()));

        fields.forEach((de, field) -> appendField(output, de, field));
        return output.toString();
    }

    private void appendField(StringBuilder output, int de, ParsedField field) {
        String value = String.valueOf(field.value());
        String metadata = "[%d bytes, offset %d]".formatted(field.length(), field.offset());

        if (!value.contains(System.lineSeparator())) {
            output.append("DE%03d %-24s = %s  %s%n"
                    .formatted(de, field.definition().key(), value, metadata));
            return;
        }

        output.append("DE%03d %s %s =%n"
                .formatted(de, field.definition().key(), metadata));
        value.lines().forEach(line -> output.append("  ").append(line).append(System.lineSeparator()));
    }

    public String rawHex() {
        return HexFormat.of().formatHex(raw);
    }
}
