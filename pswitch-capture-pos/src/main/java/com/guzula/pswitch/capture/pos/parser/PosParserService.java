package com.guzula.pswitch.capture.pos.parser;

import org.springframework.stereotype.Service;

/**
 * Referência: pos-parser.service.ts + pos-parser.config.ts (guzula-switch).
 * Usa FieldDef diretamente (re-exportado de iso-codec no original).
 * Para DEs com lógica customizada (DE 55 EMV tags, DE 47 subfields), ver
 * padrão des/ com DeParser/DePacker injetados via Map.
 *
 * TODO: portar schema e parsing.
 */
@Service
public class PosParserService {

    public Object parse(byte[] raw) {
        throw new UnsupportedOperationException("TODO: portar pos-parser.service.ts");
    }
}
