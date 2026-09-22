package com.guzula.pswitch.shared.codec;

import java.util.Arrays;
import java.util.Objects;

/** Leitor sequencial de bytes com controle centralizado de limites. */
public final class ByteCursor {

    private final byte[] source;
    private int position;

    public ByteCursor(byte[] source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public byte[] readBytes(int length, String fieldName) {
        if (length < 0) {
            throw new IllegalArgumentException("Tamanho não pode ser negativo");
        }
        ensureAvailable(length, fieldName);
        byte[] result = Arrays.copyOfRange(source, position, position + length);
        position += length;
        return result;
    }

    public int position() {
        return position;
    }

    public int remaining() {
        return source.length - position;
    }

    public boolean hasRemaining() {
        return remaining() > 0;
    }

    private void ensureAvailable(int length, String fieldName) {
        if (length > remaining()) {
            throw new IsoParseException(
                    "%s incompleto no offset %d: esperados %d bytes, restam %d"
                            .formatted(fieldName, position, length, remaining()));
        }
    }
}
