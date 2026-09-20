package com.guzula.pswitch.shared.codec;

/**
 * Tipos de campo suportados pelo codec ISO 8583.
 * Referência: IsoFieldType em src/shared/utils/iso-codec.ts (guzula-switch).
 */
public enum IsoFieldType {
    ASC,
    BIN,
    BIN_NUM,
    BCD,
    BCD_NUM,
    BCD_NUM_D02,
    EBCDIC,
    EBCDIC_NUM,
    /** numérico BCD compactado, truncado ao número de dígitos */
    BCN,
    /** Track 2 — mesma codificação de BCN */
    BCZ,
    TLV
}
