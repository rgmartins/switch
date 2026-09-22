package com.guzula.pswitch.capture.pos.parser;

import com.guzula.pswitch.capture.pos.parser.de12.De12Parser;
import com.guzula.pswitch.capture.pos.parser.de47.De47Parser;
import com.guzula.pswitch.capture.pos.parser.de55.De55Parser;
import com.guzula.pswitch.capture.pos.parser.de60.De60Parser;
import com.guzula.pswitch.capture.pos.parser.de61.De61Parser;
import com.guzula.pswitch.capture.pos.parser.de62.De62Parser;
import com.guzula.pswitch.shared.codec.ParsedField;
import com.guzula.pswitch.shared.util.PrettyJson;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/** Visão tipada da PosMessage, com cada campo nomeado pelo DE ISO que o originou. */
public record PosTransaction(
        String mti,
        String de003ProcessingCode,
        BigDecimal de004AmountTransaction,
        BigInteger de011Nsu,
        De12Parser.TransactionDateTime de012TimeLocalTransaction,
        String de018Mcc,
        String de022PosEntryMode,
        String de023CardSequenceNumber,
        String de024HostFlow,
        String de035Track2Data,
        String de038AuthorizationCode,
        String de039ResponseCode,
        String de041TerminalId,
        BigInteger de042MerchantId,
        BigInteger de043MerchantLocation,
        De47Parser.Data de047Statistics,
        String de049CurrencyCode,
        String de052PinBlock,
        String de053SecurityRelatedInfo,
        De55Parser.Data de055IccData,
        De60Parser.Data de060AdditionalPosInformation,
        De61Parser.Data de061PosPrivateData,
        De62Parser.Data de062NetworkPrivateData1,
        String de063NetworkPrivateData2,
        byte[] de064Mac
) {

    public static PosTransaction from(PosMessage message) {
        Map<Integer, ParsedField> fields = message.fields();

        String de003ProcessingCode = value(fields, 3);
        BigDecimal de004AmountTransaction = value(fields, 4);
        BigInteger de011Nsu = value(fields, 11);
        De12Parser.TransactionDateTime de012TimeLocalTransaction = value(fields, 12);
        String de018Mcc = value(fields, 18);
        String de022PosEntryMode = value(fields, 22);
        String de023CardSequenceNumber = value(fields, 23);
        String de024HostFlow = value(fields, 24);
        String de035Track2Data = value(fields, 35);
        String de038AuthorizationCode = value(fields, 38);
        String de039ResponseCode = value(fields, 39);
        String de041TerminalId = value(fields, 41);
        BigInteger de042MerchantId = value(fields, 42);
        BigInteger de043MerchantLocation = value(fields, 43);
        De47Parser.Data de047Statistics = value(fields, 47);
        String de049CurrencyCode = value(fields, 49);
        String de052PinBlock = value(fields, 52);
        String de053SecurityRelatedInfo = value(fields, 53);
        De55Parser.Data de055IccData = value(fields, 55);
        De60Parser.Data de060AdditionalPosInformation = value(fields, 60);
        De61Parser.Data de061PosPrivateData = value(fields, 61);
        De62Parser.Data de062NetworkPrivateData1 = value(fields, 62);
        String de063NetworkPrivateData2 = value(fields, 63);
        byte[] de064Mac = raw(fields, 64);

        return new PosTransaction(
                message.mti(),
                de003ProcessingCode,
                de004AmountTransaction,
                de011Nsu,
                de012TimeLocalTransaction,
                de018Mcc,
                de022PosEntryMode,
                de023CardSequenceNumber,
                de024HostFlow,
                de035Track2Data,
                de038AuthorizationCode,
                de039ResponseCode,
                de041TerminalId,
                de042MerchantId,
                de043MerchantLocation,
                de047Statistics,
                de049CurrencyCode,
                de052PinBlock,
                de053SecurityRelatedInfo,
                de055IccData,
                de060AdditionalPosInformation,
                de061PosPrivateData,
                de062NetworkPrivateData1,
                de063NetworkPrivateData2,
                de064Mac);
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(Map<Integer, ParsedField> fields, int de) {
        ParsedField field = fields.get(de);
        return field == null ? null : (T) field.value();
    }

    private static byte[] raw(Map<Integer, ParsedField> fields, int de) {
        ParsedField field = fields.get(de);
        return field == null ? null : field.raw();
    }

    @Override
    public String toString() {
        return PrettyJson.format(this);
    }
}
