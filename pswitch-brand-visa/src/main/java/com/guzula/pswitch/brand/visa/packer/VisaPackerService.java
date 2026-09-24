package com.guzula.pswitch.brand.visa.packer;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.iso8583.Iso8583Codec;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

/**
 * Monta a mensagem Visa VXFS de uma venda: cabeçalho + MTI 0100 + bitmap + DE2 (PAN), DE3 (código
 * de processamento), DE4 (valor), DE11 (NSU), DE41 (terminal) e DE42 (estabelecimento) — escopo
 * mínimo pra validar a conexão com o simulador. Referência: visa-packer.service.ts +
 * visa-sale.handler.ts + visa.helpers.ts (guzula-switch).
 *
 * <p>TODO: demais DEs (22, 23, 25, 26, 35, 37, 43, 49, 55...), MTI de reversão, e o cálculo real do
 * DE3 (hoje fixo em "000000" — falta portar a tabela VISA_DE003).
 */
@Service
public class VisaPackerService {

  private static final String MTI_SALE = "0100";
  private static final String SOURCE_STATION_ID = "264503"; // buildVisaHeader() — visa.helpers.ts
  private static final String DEFAULT_PROCESSING_CODE = "000000";

  public byte[] pack(CanonicalTransaction canonical) {
    ByteArrayOutputStream deBuffers = new ByteArrayOutputStream();
    appendPan(deBuffers, canonical.getCard().getCardNumber());
    deBuffers.writeBytes(Iso8583Codec.encodeBCD(DEFAULT_PROCESSING_CODE));
    deBuffers.writeBytes(Iso8583Codec.encodeBCD(canonical.getOperation().getAmount(), 6));
    deBuffers.writeBytes(Iso8583Codec.encodeBCD(Long.parseLong(canonical.getNsu()), 3));
    deBuffers.writeBytes(Iso8583Codec.encodeEBCDIC(canonical.getTerminalId(), 8));
    deBuffers.writeBytes(
        Iso8583Codec.encodeEBCDICNumeric(
            Long.parseLong(canonical.getMerchant().getMerchant()), 15));

    byte[] deBytes = deBuffers.toByteArray();
    byte[] mti = Iso8583Codec.encodeBCD(MTI_SALE);
    byte[] bitmap = bitmapFor(2, 3, 4, 11, 41, 42);
    int messageSize = mti.length + bitmap.length + deBytes.length;

    ByteArrayOutputStream message = new ByteArrayOutputStream();
    message.writeBytes(header(messageSize));
    message.writeBytes(mti);
    message.writeBytes(bitmap);
    message.writeBytes(deBytes);
    return message.toByteArray();
  }

  /** DE2 — LLVAR BCD: 1 byte com a quantidade de dígitos do PAN, seguido do PAN em BCD. */
  private void appendPan(ByteArrayOutputStream out, String pan) {
    out.write(pan.length());
    out.writeBytes(Iso8583Codec.encodeBCD(pan));
  }

  private byte[] header(int messageSize) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(22); // headerLength
    out.writeBytes(Iso8583Codec.encodeBCD("01")); // flagAndFormat
    out.writeBytes(Iso8583Codec.encodeBCD("02")); // textFormat
    out.write((messageSize >> 8) & 0xff); // messageSize (BIN, 2 bytes)
    out.write(messageSize & 0xff);
    out.writeBytes(Iso8583Codec.encodeBCD("000000")); // destinationStationId
    out.writeBytes(Iso8583Codec.encodeBCD(SOURCE_STATION_ID)); // sourceStationId
    out.writeBytes(Iso8583Codec.encodeBCD("00")); // roundTripControlInformation
    out.writeBytes(Iso8583Codec.encodeBCD("0000")); // baseFlags
    out.writeBytes(Iso8583Codec.encodeBCD("000000")); // messageStatusFlags
    out.writeBytes(Iso8583Codec.encodeBCD("00")); // batchNumber
    out.writeBytes(Iso8583Codec.encodeBCD("000000")); // reserved
    out.writeBytes(Iso8583Codec.encodeBCD("00")); // userInformation
    return out.toByteArray();
  }

  private byte[] bitmapFor(int... dataElements) {
    byte[] bitmap = new byte[8];
    for (int de : dataElements) {
      int byteIndex = (de - 1) / 8;
      int bitIndex = (de - 1) % 8;
      bitmap[byteIndex] |= (byte) (0x80 >> bitIndex);
    }
    return bitmap;
  }
}
