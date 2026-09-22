package com.guzula.pswitch.shared.codec;

import java.util.Arrays;
import java.util.HexFormat;

/** Bitmap ISO com numeração de bits iniciada em 1. */
public final class IsoBitmap {

  private final byte[] bytes;

  public IsoBitmap(byte[] bytes) {
    if (bytes.length != 8 && bytes.length != 16) {
      throw new IllegalArgumentException("Bitmap deve possuir 8 ou 16 bytes");
    }
    this.bytes = bytes.clone();
  }

  public boolean isSet(int bit) {
    if (bit < 1 || bit > bytes.length * 8) {
      return false;
    }
    int byteIndex = (bit - 1) / 8;
    int mask = 0x80 >>> ((bit - 1) % 8);
    return (Byte.toUnsignedInt(bytes[byteIndex]) & mask) != 0;
  }

  public int bitCount() {
    return bytes.length * 8;
  }

  public byte[] bytes() {
    return bytes.clone();
  }

  public String hex() {
    return HexFormat.of().formatHex(bytes);
  }

  @Override
  public String toString() {
    return hex();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof IsoBitmap bitmap && Arrays.equals(bytes, bitmap.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }
}
