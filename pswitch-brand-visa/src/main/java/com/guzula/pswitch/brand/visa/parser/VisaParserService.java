package com.guzula.pswitch.brand.visa.parser;

import org.springframework.stereotype.Service;

/**
 * Referência: visa-parser.service.ts + visa-parser.config.ts (guzula-switch).
 * VisaFieldDef estende FieldDef com `divideByTwo` (LLVAR onde o prefixo conta nibbles).
 *
 * TODO: portar schema de campos e parsing.
 */
@Service
public class VisaParserService {

    public Object parse(byte[] raw) {
        throw new UnsupportedOperationException("TODO: portar visa-parser.service.ts");
    }
}
