package com.guzula.pswitch.shared.codec;

/** Erro de estrutura de uma mensagem binária/ISO. */
public class IsoParseException extends IllegalArgumentException {

  public IsoParseException(String message) {
    super(message);
  }
}
