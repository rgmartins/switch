package com.guzula.pswitch.shared.codec;

/** Envelope de subcampo: tamanho BCD, identificador BCD e corpo binário. */
public record BcdSubfield(
        String id,
        int offset,
        int declaredLength,
        byte[] body
) {

    public BcdSubfield {
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
