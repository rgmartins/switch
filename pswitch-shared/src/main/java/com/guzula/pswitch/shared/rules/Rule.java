package com.guzula.pswitch.shared.rules;

/**
 * Regra global de negação/observação: código de observação interno, descrição e código de resposta
 * ISO 8583 (DE 39) a devolver ao terminal quando a regra é violada. Referência: obs.ts
 * (guzula-switch).
 *
 * @param responseCode código ISO 8583 (DE 39); {@code null} cai no fallback '96' em {@link Obs}.
 */
public record Rule(int obsCode, String description, String responseCode) {}
