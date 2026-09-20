package com.guzula.pswitch.brand.visa.packer;

import org.springframework.stereotype.Service;

/**
 * Referência: visa-packer.service.ts (guzula-switch). Inverso do VisaParserService.
 *
 * TODO: portar geração de bitmap e serialização.
 */
@Service
public class VisaPackerService {

    public byte[] pack(Object parsedMessage) {
        throw new UnsupportedOperationException("TODO: portar visa-packer.service.ts");
    }
}
