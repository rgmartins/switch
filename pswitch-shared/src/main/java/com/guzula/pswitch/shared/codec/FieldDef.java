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
        LengthFormat format,
        Integer length
) {

    public FieldDef {
        if (de != null && (de < 2 || de > 128)) {
            throw new IllegalArgumentException("DE deve estar entre 2 e 128");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("A chave do campo é obrigatória");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do campo é obrigatório");
        }
        if (type == null || format == null) {
            throw new IllegalArgumentException("Tipo e formato do campo são obrigatórios");
        }
        if (format == LengthFormat.FIXED && (length == null || length < 0)) {
            throw new IllegalArgumentException("Campo FIXED exige tamanho");
        }
    }
}
