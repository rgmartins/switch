package com.guzula.pswitch.capture.pos.parser;

import com.guzula.pswitch.capture.pos.PosService;
import com.guzula.pswitch.capture.pos.parser.de47.De47Parser;
import com.guzula.pswitch.capture.pos.parser.de55.De55Parser;
import com.guzula.pswitch.capture.pos.parser.de60.De60Parser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosParserServiceTest {

    private static final String MESSAGE_HEX = """
            600070940a6f12003030470020e29a1d00101000000000360070
            9227231020093411591260201010700000000140353535353535
            3535353535353535353535353535353535353535353535353535
            3535353535353535303038393135393200101695481300010800
            101695481300010105005001017092264f4b0000000000000000
            0000000020231020092905ffff010019312020203031314f4b30
            202020202020014f4b0006020170922626002305053981998912
            00010000315855294e2020200000012600180670922610163501
            12390001002020200008098652525252525252521103fffff170
            0068a060031c0126012407820239009f2701809f2608835973b1
            0868d1b29f3602004e950500000480009f34030203009f370407
            cb34b29f3303e0f8c85f280200769f10120110a0400322000000
            0000000000000000ff9a032310205f3401005f201a4b454c4c59
            2053494c564120414e4452414445202020202020208407a00000
            00041010001700151254699703fffff1700168a060069c008300
            1301434931364e53503933343054001402124a39413530333137
            38313038000708020200000000000314100000311605ffffffff
            ffffffff20383935353035333237333030313939353433313900
            0328000200640062057092262310200929053030300039950502
            000480009f37049d4b22589f2701409f26085405f0aef3332917
            9f100706010a036000000000383632303035078f2e32b303a240
            """.replaceAll("\\s", "");

    private final PosParserService parser = new PosParserService();

    @Test
    void opensCompleteSimulatorMessage() {
        byte[] payload = HexFormat.of().parseHex(MESSAGE_HEX);

        PosMessage message = parser.parse(payload);

        assertEquals(546, message.raw().length);
        assertEquals(new PosTpdu("60", "0070", "940a"), message.tpdu());
        assertEquals("6f", message.transparency());
        assertEquals("1200", message.mti());
        assertEquals("3030470020e29a1d", message.bitmap().hex());
        assertEquals("001010", message.fields().get(3).value());
        assertEquals(new BigDecimal("36.00"), message.fields().get(4).value());
        assertEquals(21, message.fields().size());
        assertArrayEquals(payload, message.raw());

        var de47 = (De47Parser.Data) message.fields().get(47).value();
        assertEquals(4, de47.subfields().size());
        assertEquals("709226", ((De47Parser.ConnectionStatistics)
                de47.subfields().get("01").details()).documentNumber());
        assertEquals("2023-10-20 09:29:05", ((De47Parser.ConnectionStatistics)
                de47.subfields().get("01").details()).dateTime());
        assertEquals(26, ((De47Parser.SupplyStatistics)
                de47.subfields().get("02").details()).lineCount());
        assertTrue(de47.subfields().get("05").details() instanceof De47Parser.FirstTransactionData);
        assertEquals("1016", ((De47Parser.ConnectionTimes)
                de47.subfields().get("06").details()).connectionTime());
        assertTrue(message.toMultilineString().contains("\"transactionCount\": 1"));
        assertTrue(message.toMultilineString().contains("        \"documentNumber\": \"709226\""));

        var de55 = (De55Parser.Data) message.fields().get(55).value();
        var emv = (De55Parser.EmvData) de55.subfields().get("07").details();
        assertEquals("3900", emv.tags().get("82"));
        assertEquals("80", emv.tags().get("9f27"));
        assertEquals("835973b10868d1b2", emv.tags().get("9f26"));
        assertEquals("a0000000041010", emv.tags().get("84"));

        var de60 = (De60Parser.Data) message.fields().get(60).value();
        var encryption = (De60Parser.TrackEncryptionData)
                de60.subfields().get("12").details();
        assertEquals("546997", encryption.cardBin());
        assertEquals("03", encryption.encryptionType().code());
        assertEquals("DUKPT Triple DES", encryption.encryptionType().description());
        assertEquals("fffff1700168a060069c", encryption.ksn());
    }

    @Test
    void rejectsTruncatedMessageWithFieldAndOffset() {
        byte[] payload = HexFormat.of().parseHex(MESSAGE_HEX);

        var error = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(Arrays.copyOf(payload, payload.length - 1)));

        assertTrue(error.getMessage().contains("DE 064"));
        assertTrue(error.getMessage().contains("offset"));
    }

    @Test
    void posServiceEchoesExactlyTheReceivedBytes() {
        byte[] payload = HexFormat.of().parseHex("60000000006f12000000000000000000");
        AtomicReference<byte[]> sent = new AtomicReference<>();
        PosService service = new PosService((connectionId, response) -> sent.set(response), parser);

        service.handleInbound("connection-1", payload);

        assertArrayEquals(payload, sent.get());
    }
}
