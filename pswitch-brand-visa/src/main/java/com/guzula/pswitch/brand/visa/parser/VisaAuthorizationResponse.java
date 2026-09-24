package com.guzula.pswitch.brand.visa.parser;

/**
 * Resultado mínimo da resposta Visa (DE3/4/11/38/39/41), suficiente pra correlacionar com a
 * transação original (terminalId + nsu) e popular {@code CanonicalTransaction.Response}.
 */
public record VisaAuthorizationResponse(
    String mti,
    String processingCode,
    long amount,
    long nsu,
    String authorizationCode,
    String responseCode,
    String terminalId) {}
