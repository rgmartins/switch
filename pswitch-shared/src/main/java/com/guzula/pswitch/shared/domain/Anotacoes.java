package com.guzula.pswitch.shared.domain;

/**
 * Catálogo de anotações conhecidas, anexadas à transação canônica quando uma
 * regra de negócio identifica algo digno de nota (sem necessariamente ser um erro).
 * Referência: src/shared/anotacoes.ts (guzula-switch).
 *
 * TODO: portar as demais anotações de anotacoes.ts conforme forem sendo necessárias.
 */
public final class Anotacoes {

    private Anotacoes() {
    }

    public static Anotacao sub6108Zeroed() {
        Anotacao anotacao = new Anotacao();
        anotacao.setId("SUB_61_08_ZEROED");
        anotacao.setError(false);
        anotacao.setDescription("Subcampo 61.08 presente mas com todos os valores zerados");
        return anotacao;
    }
}
