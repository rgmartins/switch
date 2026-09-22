package com.guzula.pswitch.shared.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Fonte única de verdade para encode/decode de campos ISO 8583.
 * Referência: parseIsoField / packIsoField em src/shared/utils/iso-codec.ts (guzula-switch).
 *
 * TODO: portar a lógica de parse/pack por IsoFieldType (BCD, BCN, BCZ, TLV, EBCDIC...).
 */
public final class IsoCodec {

    private static final Charset EBCDIC = Charset.forName("IBM037");

    private IsoCodec() {
    }

    public static Object parseIsoField(byte[] slice, IsoFieldType type, Integer digits) {
        return switch (type) {
            case BIN -> HexFormat.of().formatHex(slice);
            case BIN_NUM -> new BigInteger(1, slice);
            case ASC -> new String(slice, StandardCharsets.US_ASCII).strip();
            case EBCDIC -> new String(slice, EBCDIC).strip();
            case BCD -> BcdCodec.decode(slice);
            case BCD_NUM -> new BigInteger(BcdCodec.decode(slice));
            case BCD_NUM_D02 -> new BigDecimal(new BigInteger(BcdCodec.decode(slice)), 2);
            case EBCDIC_NUM -> new BigInteger(new String(slice, EBCDIC).strip());
            case BCN, BCZ -> compactedDigits(slice, digits);
            case TLV -> BerTlvParser.parse(slice);
        };
    }

    public static byte[] packIsoField(String value, IsoFieldType type, Integer length, boolean rightPad) {
        throw new UnsupportedOperationException("TODO: portar packIsoField de iso-codec.ts");
    }

    private static String compactedDigits(byte[] slice, Integer digits) {
        String value = HexFormat.of().withUpperCase().formatHex(slice);
        if (digits == null || digits >= value.length()) {
            return value;
        }
        return value.substring(value.length() - digits);
    }
}
