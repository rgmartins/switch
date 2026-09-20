package com.guzula.pswitch.shared.util;

/**
 * Codec de tipos estilo COBOL (ex.: V99 — numérico implícito de 2 casas decimais)
 * usado pelos protocolos sequenciais ASCII (tarifas, pré-autorização).
 * Referência: src/shared/utils/cobol-codec.ts (guzula-switch).
 *
 * TODO: portar os tipos de campo (V99, etc.) conforme forem necessários.
 */
public final class CobolCodec {

    private CobolCodec() {
    }
}
