package com.guzula.pswitch.shared.codec;

/** Forma usada para determinar o tamanho de um campo. */
public enum LengthFormat {
    FIXED(0),
    LLVAR(1),
    LLLVAR(2);

    private final int prefixBytes;

    LengthFormat(int prefixBytes) {
        this.prefixBytes = prefixBytes;
    }

    public int prefixBytes() {
        return prefixBytes;
    }
}
