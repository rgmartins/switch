package com.guzula.pswitch.shared.codec;

import java.util.ArrayList;
import java.util.List;

/**
 * Abre uma sequência de subcampos cujo tamanho ocupa dois bytes BCD e inclui
 * o identificador de um byte BCD.
 */
public final class BcdSubfieldReader {

    private static final int LENGTH_BYTES = 2;
    private static final int ID_BYTES = 1;

    private BcdSubfieldReader() {
    }

    public static List<BcdSubfield> readAll(byte[] source, String fieldName) {
        ByteCursor cursor = new ByteCursor(source);
        List<BcdSubfield> result = new ArrayList<>();

        while (cursor.hasRemaining()) {
            int offset = cursor.position();
            int declaredLength = BcdCodec.decodeLength(
                    cursor.readBytes(LENGTH_BYTES, fieldName + ".tamanho"),
                    fieldName);
            if (declaredLength < ID_BYTES) {
                throw new IsoParseException(
                        "%s possui subcampo com tamanho inválido no offset %d"
                                .formatted(fieldName, offset));
            }

            String id = BcdCodec.decode(cursor.readBytes(ID_BYTES, fieldName + ".id"));
            byte[] body = cursor.readBytes(
                    declaredLength - ID_BYTES,
                    "%s.subcampo_%s".formatted(fieldName, id));
            result.add(new BcdSubfield(id, offset, declaredLength, body));
        }

        return List.copyOf(result);
    }
}
