package com.guzula.pswitch.shared.domain;

import java.util.Map;

/**
 * Uma interação (request/response) com uma das partes externas (POS, bandeira, HSM, antifraude...).
 * Referência: src/shared/domain/canonical-transaction.ts (guzula-switch) — interface Interacao.
 */
public class Interacao {

    private String id; // ParteInteracao: pos, visa, mastercard, amex, hsm, antifraud, planet, tarifas, pre-auth, historico
    private String sentido; // TypeInteractionDirection: request | response
    private boolean sanitizado;
    private InteracaoTempo time;
    private Map<String, Object> message;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSentido() {
        return sentido;
    }

    public void setSentido(String sentido) {
        this.sentido = sentido;
    }

    public boolean isSanitizado() {
        return sanitizado;
    }

    public void setSanitizado(boolean sanitizado) {
        this.sanitizado = sanitizado;
    }

    public InteracaoTempo getTime() {
        return time;
    }

    public void setTime(InteracaoTempo time) {
        this.time = time;
    }

    public Map<String, Object> getMessage() {
        return message;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }
}
