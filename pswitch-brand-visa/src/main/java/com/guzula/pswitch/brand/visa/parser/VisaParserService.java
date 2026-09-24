package com.guzula.pswitch.brand.visa.parser;

import com.guzula.pswitch.shared.iso8583.Iso8583Codec;
import org.springframework.stereotype.Service;

/**
 * Lê a resposta Visa VXFS (cabeçalho + MTI + bitmap + DEs). Escopo mínimo, espelhando exatamente o
 * que o simulador (switch-simuladores) devolve hoje: DE3, DE4, DE11, DE38, DE39, DE41 — o
 * suficiente pra correlacionar a resposta com a transação original e popular o resultado da
 * autorização. Referência: visa-parser.service.ts (guzula-switch).
 *
 * <p>TODO: demais DEs, bitmap secundário e Reject Message Header, quando forem necessários.
 */
@Service
public class VisaParserService {

  private static final int HEADER_LENGTH = 22;

  public VisaAuthorizationResponse parse(byte[] raw) {
    int offset = HEADER_LENGTH;
    String mti = Iso8583Codec.decodeBCD(slice(raw, offset, 2));
    offset += 2;
    byte[] bitmap = slice(raw, offset, 8);
    offset += 8;

    String processingCode = null;
    long amount = 0;
    long nsu = 0;
    String authorizationCode = null;
    String responseCode = null;
    String terminalId = null;

    if (isBitSet(bitmap, 3)) {
      processingCode = Iso8583Codec.decodeBCD(slice(raw, offset, 3));
      offset += 3;
    }
    if (isBitSet(bitmap, 4)) {
      amount = Long.parseLong(Iso8583Codec.decodeBCD(slice(raw, offset, 6)));
      offset += 6;
    }
    if (isBitSet(bitmap, 11)) {
      nsu = Long.parseLong(Iso8583Codec.decodeBCD(slice(raw, offset, 3)));
      offset += 3;
    }
    if (isBitSet(bitmap, 38)) {
      authorizationCode = Iso8583Codec.decodeEBCDIC(slice(raw, offset, 6));
      offset += 6;
    }
    if (isBitSet(bitmap, 39)) {
      responseCode = Iso8583Codec.decodeEBCDIC(slice(raw, offset, 2));
      offset += 2;
    }
    if (isBitSet(bitmap, 41)) {
      terminalId = Iso8583Codec.decodeEBCDIC(slice(raw, offset, 8));
      offset += 8;
    }

    return new VisaAuthorizationResponse(
        mti, processingCode, amount, nsu, authorizationCode, responseCode, terminalId);
  }

  private byte[] slice(byte[] raw, int offset, int length) {
    if (offset + length > raw.length) {
      throw new IllegalArgumentException(
          "Resposta Visa incompleta: esperado %d bytes a partir do offset %d, restam %d"
              .formatted(length, offset, Math.max(raw.length - offset, 0)));
    }
    byte[] out = new byte[length];
    System.arraycopy(raw, offset, out, 0, length);
    return out;
  }

  private boolean isBitSet(byte[] bitmap, int de) {
    int byteIndex = (de - 1) / 8;
    int bitIndex = (de - 1) % 8;
    return (bitmap[byteIndex] & (0x80 >> bitIndex)) != 0;
  }
}
