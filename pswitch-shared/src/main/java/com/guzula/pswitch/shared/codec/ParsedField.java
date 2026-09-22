package com.guzula.pswitch.shared.codec;

import java.util.HexFormat;

/** Campo aberto, mantendo valor interpretado e bytes originais. */
public record ParsedField(
        FieldDef definition,
        int offset,
        int length,
        byte[] raw,
        Object value
) {

    public ParsedField {
        raw = raw.clone();
    }

    @Override
    public byte[] raw() {
        return raw.clone();
    }

    public String rawHex() {
        return HexFormat.of().formatHex(raw);
    }
}
