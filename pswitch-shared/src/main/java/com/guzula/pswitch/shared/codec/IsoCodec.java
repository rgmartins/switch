package com.guzula.pswitch.shared.codec;

/**
 * Fonte única de verdade para encode/decode de campos ISO 8583.
 * Referência: parseIsoField / packIsoField em src/shared/utils/iso-codec.ts (guzula-switch).
 *
 * TODO: portar a lógica de parse/pack por IsoFieldType (BCD, BCN, BCZ, TLV, EBCDIC...).
 */
public final class IsoCodec {

    private IsoCodec() {
    }

    public static String parseIsoField(byte[] slice, IsoFieldType type, Integer length) {
        throw new UnsupportedOperationException("TODO: portar parseIsoField de iso-codec.ts");
    }

    public static byte[] packIsoField(String value, IsoFieldType type, Integer length, boolean rightPad) {
        throw new UnsupportedOperationException("TODO: portar packIsoField de iso-codec.ts");
    }
}
