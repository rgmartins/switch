package com.guzula.pswitch.capture.pos.parser.de12;

import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.FieldValueDecoder;
import com.guzula.pswitch.shared.codec.IsoParseException;
import com.guzula.pswitch.shared.util.PrettyJson;

/** Abre a data e a hora local da transacao informadas no DE 12. */
public final class De12Parser implements FieldValueDecoder {

    private static final int DE12_LENGTH = 6;

    @Override
    public Object decode(byte[] raw, FieldDef definition) {
        if (raw.length != DE12_LENGTH) {
            throw new IsoParseException(
                    "DE012 deve possuir 6 bytes, mas recebeu %d".formatted(raw.length));
        }

        String value = BcdCodec.decode(raw);
        return new TransactionDateTime(
                value,
                "%s-%s-%s".formatted(
                        value.substring(0, 2),
                        value.substring(2, 4),
                        value.substring(4, 6)),
                "%s:%s:%s".formatted(
                        value.substring(6, 8),
                        value.substring(8, 10),
                        value.substring(10, 12)));
    }

    public record TransactionDateTime(
            String raw,
            String date,
            String time) {

        @Override
        public String toString() {
            return PrettyJson.format(this);
        }
    }
}
