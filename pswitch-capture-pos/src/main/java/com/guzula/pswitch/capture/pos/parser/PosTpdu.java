package com.guzula.pswitch.capture.pos.parser;

/** Cabeçalho TPDU do POS. */
public record PosTpdu(String id, String destination, String origin) {}
