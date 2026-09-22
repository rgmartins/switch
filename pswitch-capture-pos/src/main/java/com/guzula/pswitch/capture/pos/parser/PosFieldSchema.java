package com.guzula.pswitch.capture.pos.parser;

import com.guzula.pswitch.shared.codec.FieldDef;
import com.guzula.pswitch.shared.codec.IsoFieldType;
import com.guzula.pswitch.shared.codec.LengthFormat;

import java.util.LinkedHashMap;
import java.util.Map;

/** Campos ISO suportados pelo canal POS. */
public final class PosFieldSchema {

    private PosFieldSchema() {
    }

    public static Map<Integer, FieldDef> fields() {
        Map<Integer, FieldDef> fields = new LinkedHashMap<>();
        add(fields, 3, "processingCode", "Processing Code", IsoFieldType.BCD, LengthFormat.FIXED, 3);
        add(fields, 4, "amountTransaction", "Amount Transaction", IsoFieldType.BCD_NUM_D02, LengthFormat.FIXED, 6);
        add(fields, 11, "nsu", "NSU", IsoFieldType.BCD_NUM, LengthFormat.FIXED, 3);
        add(fields, 12, "timeLocalTransaction", "Time, Local Transaction", IsoFieldType.BCD, LengthFormat.FIXED, 6);
        add(fields, 18, "mcc", "MCC", IsoFieldType.BCD, LengthFormat.FIXED, 2);
        add(fields, 22, "posEntryMode", "POS Entry Mode", IsoFieldType.BCD, LengthFormat.FIXED, 6);
        add(fields, 23, "cardSequenceNumber", "Card Sequence Number", IsoFieldType.BCD, LengthFormat.FIXED, 1);
        add(fields, 24, "hostFlow", "Host Flow", IsoFieldType.BCD, LengthFormat.FIXED, 2);
        add(fields, 35, "track2Data", "Track 2 Data", IsoFieldType.BIN, LengthFormat.LLVAR, null);
        add(fields, 38, "authorizationCode", "Authorization Code", IsoFieldType.ASC, LengthFormat.FIXED, 6);
        add(fields, 39, "responseCode", "Response Code", IsoFieldType.ASC, LengthFormat.FIXED, 2);
        add(fields, 41, "terminalId", "Card Acceptor Terminal ID", IsoFieldType.ASC, LengthFormat.FIXED, 8);
        add(fields, 42, "merchantId", "Merchant ID", IsoFieldType.BCD_NUM, LengthFormat.FIXED, 8);
        add(fields, 43, "merchantLocation", "Merchant Location", IsoFieldType.BCD_NUM, LengthFormat.LLVAR, null);
        add(fields, 47, "statistics", "Statistics", IsoFieldType.BIN, LengthFormat.LLLVAR, null);
        add(fields, 49, "currencyCode", "Currency Code", IsoFieldType.BIN, LengthFormat.FIXED, 2);
        add(fields, 52, "pinBlock", "PIN Block", IsoFieldType.BIN, LengthFormat.FIXED, 8);
        add(fields, 53, "securityRelatedInfo", "Security Related Info", IsoFieldType.BIN, LengthFormat.LLVAR, null);
        add(fields, 55, "iccData", "ICC Data (EMV)", IsoFieldType.BIN, LengthFormat.LLLVAR, null);
        add(fields, 60, "additionalPosInformation", "Additional POS Information", IsoFieldType.BIN, LengthFormat.LLLVAR, null);
        add(fields, 61, "posPrivateData", "POS Private Data", IsoFieldType.BIN, LengthFormat.LLLVAR, null);
        add(fields, 62, "networkPrivateData1", "Network Private Data 1", IsoFieldType.BIN, LengthFormat.LLLVAR, null);
        add(fields, 63, "networkPrivateData2", "Network Private Data 2", IsoFieldType.BIN, LengthFormat.LLLVAR, null);
        add(fields, 64, "mac", "MAC", IsoFieldType.BIN, LengthFormat.FIXED, 8);
        return Map.copyOf(fields);
    }

    private static void add(
            Map<Integer, FieldDef> fields,
            int de,
            String key,
            String name,
            IsoFieldType type,
            LengthFormat format,
            Integer length) {
        fields.put(de, new FieldDef(de, key, name, type, format, length));
    }
}
