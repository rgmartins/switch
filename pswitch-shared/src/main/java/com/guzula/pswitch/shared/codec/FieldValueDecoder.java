package com.guzula.pswitch.shared.codec;

@FunctionalInterface
public interface FieldValueDecoder {
  Object decode(byte[] raw, FieldDef definition);
}
