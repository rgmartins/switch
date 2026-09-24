package com.guzula.pswitch.brand.visa.packer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.iso8583.Iso8583Codec;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class VisaPackerServiceTest {

  @Test
  void packsHeaderMtiBitmapAndTheMinimumDataElements() {
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setNsu("1000");
    canonical.setTerminalId("00891592");

    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4111111111111111"); // 16 dígitos
    canonical.setCard(card);

    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setAmount(3600);
    canonical.setOperation(operation);

    CanonicalTransaction.Merchant merchant = new CanonicalTransaction.Merchant();
    merchant.setMerchant("10169548130001"); // 14 dígitos -> preenchido com 1 zero à esquerda
    canonical.setMerchant(merchant);

    byte[] message = new VisaPackerService().pack(canonical);

    // Cabeçalho (22 bytes) + MTI (2) + bitmap (8) = 32 bytes antes dos DEs.
    assertEquals(22, message[0] & 0xff); // headerLength
    assertEquals("0100", Iso8583Codec.decodeBCD(Arrays.copyOfRange(message, 22, 24)));

    byte[] bitmap = Arrays.copyOfRange(message, 24, 32);
    for (int de : new int[] {2, 3, 4, 11, 41, 42}) {
      assertBitSet(bitmap, de);
    }

    int offset = 32;
    int panDigits = message[offset] & 0xff;
    assertEquals(16, panDigits);
    offset += 1;
    String pan =
        Iso8583Codec.decodeBCD(Arrays.copyOfRange(message, offset, offset + panDigits / 2));
    assertEquals("4111111111111111", pan);
    offset += panDigits / 2;

    assertEquals("000000", Iso8583Codec.decodeBCD(Arrays.copyOfRange(message, offset, offset + 3)));
    offset += 3;

    assertEquals(
        "000000003600", Iso8583Codec.decodeBCD(Arrays.copyOfRange(message, offset, offset + 6)));
    offset += 6;

    assertEquals("001000", Iso8583Codec.decodeBCD(Arrays.copyOfRange(message, offset, offset + 3)));
    offset += 3;

    assertEquals(
        "00891592", Iso8583Codec.decodeEBCDIC(Arrays.copyOfRange(message, offset, offset + 8)));
    offset += 8;

    assertEquals(
        "010169548130001",
        Iso8583Codec.decodeEBCDIC(Arrays.copyOfRange(message, offset, offset + 15)));
    offset += 15;

    assertEquals(message.length, offset);
  }

  private void assertBitSet(byte[] bitmap, int de) {
    int byteIndex = (de - 1) / 8;
    int bitIndex = (de - 1) % 8;
    assertTrue(
        (bitmap[byteIndex] & (0x80 >> bitIndex)) != 0, "Esperava bit do DE" + de + " ligado");
  }
}
