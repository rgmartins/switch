package com.guzula.pswitch.shared.domain;

/**
 * Referência: src/shared/domain/canonical-transaction.ts (guzula-switch) — interface Anotacao.
 */
public class Anotacao {

    private String id;
    private boolean error;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
