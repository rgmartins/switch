package com.guzula.pswitch.capture.pos.parser;

import com.guzula.pswitch.capture.pos.PosMapperService;
import com.guzula.pswitch.capture.pos.PosService;
import com.guzula.pswitch.capture.pos.parser.de12.De12Parser;
import com.guzula.pswitch.capture.pos.parser.de47.De47Parser;
import com.guzula.pswitch.capture.pos.parser.de55.De55Parser;
import com.guzula.pswitch.capture.pos.parser.de60.De60Parser;
import com.guzula.pswitch.capture.pos.parser.de61.De61Parser;
import com.guzula.pswitch.capture.pos.parser.de62.De62Parser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        var de12 = (De12Parser.TransactionDateTime) message.fields().get(12).value();
        assertEquals("231020093411", de12.raw());
        assertEquals("23-10-20", de12.date());
        assertEquals("09:34:11", de12.time());

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

        var de61 = (De61Parser.Data) message.fields().get(61).value();
        assertEquals(6, de61.subfields().size());
        assertEquals("CI16NSP9340T", ((De61Parser.SoftwareIdentification)
                de61.subfields().get("01").details()).softwareId());
        assertEquals("J9A503178108", ((De61Parser.PosSerialNumber)
                de61.subfields().get("02").details()).serialNumber());
        var financing = (De61Parser.FinancingData) de61.subfields().get("08").details();
        assertEquals("02", financing.financingType().code());
        assertEquals(2, financing.installmentCount());
        assertEquals("000000", financing.preDatedDate());
        assertEquals("00", financing.numberOfDays());
        assertEquals("1000", ((De61Parser.MatrixProduct)
                de61.subfields().get("14").details()).productCode());
        var identification = (De61Parser.PositiveIdentification)
                de61.subfields().get("16").details();
        assertEquals("05", identification.questionCode());
        assertEquals("ffffffffffffffff", identification.nextFieldMask());
        assertEquals(20, identification.responseLength());
        assertEquals("89550532730019954319", identification.response());
        assertEquals("0002", ((De61Parser.SecondaryProduct)
                de61.subfields().get("28").details()).productCode());

        var de62 = (De62Parser.Data) message.fields().get(62).value();
        assertEquals(1, de62.subfields().size());
        var confirmation = (De62Parser.EmvConfirmation)
                de62.subfields().get("05").details();
        assertEquals("709226", confirmation.documentNumber());
        assertEquals("231020092905", confirmation.transactionDateTime());
        assertEquals("000", confirmation.responseCode());
        assertEquals(39, confirmation.emvLength());
        assertEquals("0200048000", confirmation.emvTags().get("95"));
        assertEquals("9d4b2258", confirmation.emvTags().get("9f37"));
        assertEquals("40", confirmation.emvTags().get("9f27"));
        assertEquals("5405f0aef3332917", confirmation.emvTags().get("9f26"));
        assertEquals("06010a03600000", confirmation.emvTags().get("9f10"));
        assertEquals("0000383632303035", confirmation.additionalDataHex());
    }

    @Test
    void buildsTypedPosTransactionFromParsedMessage() {
        byte[] payload = HexFormat.of().parseHex(MESSAGE_HEX);
        PosMessage message = parser.parse(payload);

        PosTransaction transaction = PosTransaction.from(message);

        // Cada DE é conferido individualmente contra a mensagem simulada: isso é a
        // rede de segurança contra uma eventual troca de posição no PosTransaction.from(),
        // já que campos do mesmo tipo (String, BigInteger) não dão erro de compilação
        // nem ClassCastException se forem trocados de lugar.
        assertEquals("001010", transaction.de003ProcessingCode());
        assertEquals(new BigDecimal("36.00"), transaction.de004AmountTransaction());
        assertEquals(new BigInteger("709227"), transaction.de011Nsu());
        assertEquals("231020093411", transaction.de012TimeLocalTransaction().raw());
        assertEquals("5912", transaction.de018Mcc());
        assertEquals("602010107000", transaction.de022PosEntryMode());
        assertEquals("00", transaction.de023CardSequenceNumber());
        assertEquals("0001", transaction.de024HostFlow());
        assertEquals(
                "35353535353535353535353535353535353535353535353535353535353535353535353535353535",
                transaction.de035Track2Data());
        assertNull(transaction.de038AuthorizationCode());
        assertNull(transaction.de039ResponseCode());
        assertEquals("00891592", transaction.de041TerminalId());
        assertEquals(new BigInteger("10169548130001"), transaction.de042MerchantId());
        assertEquals(new BigInteger("10169548130001"), transaction.de043MerchantLocation());
        assertEquals("709226", ((De47Parser.ConnectionStatistics)
                transaction.de047Statistics().subfields().get("01").details()).documentNumber());
        assertEquals("0986", transaction.de049CurrencyCode());
        assertEquals("5252525252525252", transaction.de052PinBlock());
        assertEquals("03fffff1700068a060031c", transaction.de053SecurityRelatedInfo());
        assertEquals("3900", ((De55Parser.EmvData)
                transaction.de055IccData().subfields().get("07").details()).tags().get("82"));
        assertEquals("546997", ((De60Parser.TrackEncryptionData)
                transaction.de060AdditionalPosInformation().subfields().get("12").details()).cardBin());
        assertEquals("CI16NSP9340T", ((De61Parser.SoftwareIdentification)
                transaction.de061PosPrivateData().subfields().get("01").details()).softwareId());
        assertEquals("709226", ((De62Parser.EmvConfirmation)
                transaction.de062NetworkPrivateData1().subfields().get("05").details()).documentNumber());
        assertNull(transaction.de063NetworkPrivateData2());
        assertArrayEquals(HexFormat.of().parseHex("078f2e32b303a240"), transaction.de064Mac());
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
        PosService service = new PosService(
                (connectionId, response) -> sent.set(response), parser, new PosMapperService());

        service.handleInbound("connection-1", payload);

        assertArrayEquals(payload, sent.get());
    }
}
