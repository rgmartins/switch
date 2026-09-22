package com.guzula.pswitch.capture.pos.parser;

import com.guzula.pswitch.capture.pos.parser.de47.De47Parser;
import com.guzula.pswitch.capture.pos.parser.de55.De55Parser;
import com.guzula.pswitch.capture.pos.parser.de60.De60Parser;
import com.guzula.pswitch.shared.codec.BcdCodec;
import com.guzula.pswitch.shared.codec.ByteCursor;
import com.guzula.pswitch.shared.codec.IsoBitmap;
import com.guzula.pswitch.shared.codec.IsoFieldsParser;
import com.guzula.pswitch.shared.codec.IsoParseException;
import org.springframework.stereotype.Service;

import java.util.HexFormat;
import java.util.Map;

/** Abre o envelope POS e delega os campos ISO ao motor compartilhado. */
@Service
public class PosParserService {

    private static final int TPDU_LENGTH = 5;
    private static final int PRIMARY_BITMAP_LENGTH = 8;

    private final IsoFieldsParser fieldsParser = new IsoFieldsParser(
            PosFieldSchema.fields(),
            Map.of(
                    47, new De47Parser(),
                    55, new De55Parser(),
                    60, new De60Parser()));

    public PosMessage parse(byte[] raw) {
        ByteCursor cursor = new ByteCursor(raw);

        PosTpdu tpdu = parseTpdu(cursor.readBytes(TPDU_LENGTH, "TPDU"));
        String transparency = HexFormat.of().formatHex(
                cursor.readBytes(1, "tipo de transparência"));
        String mti = BcdCodec.decode(cursor.readBytes(2, "MTI"));
        IsoBitmap bitmap = new IsoBitmap(cursor.readBytes(PRIMARY_BITMAP_LENGTH, "bitmap primário"));

        if (bitmap.isSet(1)) {
            throw new IsoParseException("Bitmap secundário ainda não é suportado pelo canal POS");
        }

        var fields = fieldsParser.parse(cursor, bitmap);
        if (cursor.hasRemaining()) {
            throw new IsoParseException(
                    "Mensagem POS possui %d bytes não consumidos no offset %d"
                            .formatted(cursor.remaining(), cursor.position()));
        }

        return new PosMessage(tpdu, transparency, mti, bitmap, fields, raw);
    }

    private PosTpdu parseTpdu(byte[] tpdu) {
        return new PosTpdu(
                HexFormat.of().formatHex(new byte[]{tpdu[0]}),
                address(tpdu, 1),
                address(tpdu, 3));
    }

    private String address(byte[] tpdu, int offset) {
        byte[] address = new byte[]{tpdu[offset], tpdu[offset + 1]};
        try {
            return BcdCodec.decode(address);
        } catch (IsoParseException ignored) {
            return HexFormat.of().formatHex(address);
        }
    }
}
