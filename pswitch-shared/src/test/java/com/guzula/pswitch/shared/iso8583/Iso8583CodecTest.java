package com.guzula.pswitch.shared.iso8583;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class Iso8583CodecTest {

  @Test
  void encodesAndDecodesBcdDigits() {
    byte[] encoded = Iso8583Codec.encodeBCD("0100");
    assertEquals("0100", HexFormat.of().formatHex(encoded));
    assertEquals("0100", Iso8583Codec.decodeBCD(encoded));
  }

  @Test
  void encodesNumberAsZeroPaddedBcd() {
    byte[] encoded = Iso8583Codec.encodeBCD(3600L, 6);
    assertEquals("000000003600", Iso8583Codec.decodeBCD(encoded));
  }

  @Test
  void roundTripsEbcdicAlphanumeric() {
    byte[] encoded = Iso8583Codec.encodeEBCDIC("TERM", 8);
    assertEquals("TERM", Iso8583Codec.decodeEBCDIC(encoded));
  }

  @Test
  void roundTripsEbcdicNumericZeroPadded() {
    byte[] encoded = Iso8583Codec.encodeEBCDICNumeric(999999999999999L, 15);
    assertEquals("999999999999999", Iso8583Codec.decodeEBCDIC(encoded));
  }
}
