package com.guzula.pswitch.shared.codec;

/**
 * Definição base de um campo de schema (DE). Cada protocolo (POS, Visa, Planet)
 * estende este contrato com seus próprios atributos (ver *-parser.config equivalentes).
 * Referência: FieldDef em src/shared/utils/iso-codec.ts (guzula-switch).
 */
public record FieldDef(
        Integer de,
        String key,
        String name,
        IsoFieldType type,
        String format,
        Integer length
) {
}
