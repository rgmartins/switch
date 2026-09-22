package com.guzula.pswitch.capture.pos.packer;

import org.springframework.stereotype.Service;

/**
 * Referência: pos-packer.service.ts (guzula-switch). Inverso do PosParserService.
 *
 * <p>TODO: portar geração de bitmap e serialização de volta pro terminal.
 */
@Service
public class PosPackerService {

  public byte[] pack(Object parsedMessage) {
    throw new UnsupportedOperationException("TODO: portar pos-packer.service.ts");
  }
}
