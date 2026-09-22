package com.guzula.pswitch.capture.pos;

/**
 * Constantes específicas do canal POS.
 * Referência: pos.constants.ts (guzula-switch).
 */
public final class PosConstants {

    private PosConstants() {
    }

    public static final class Mti {
        public static final String UNDO = "1400";
        public static final String REVERSAL = "1420";
        public static final String SALE = "1200";

        private Mti() {
        }
    }

    public static final class ProcessingCode {
        public static final String NAO_SEI_650000 = "650000";
        public static final String NAO_SEI_660000 = "660000";
        public static final String CDC_CONSULTING_DEBIT = "380010";

        private ProcessingCode() {
        }
    }

    public static final class HostFlow {
        public static final int PRE_AUTHORIZATION = 3;
        public static final int CDC_QUERY = 6;
        public static final int BALANCE_QUERY = 13;
        public static final int SOLUTION_SALE_AND_VISA_VALE_4 = 4;
        public static final int SOLUTION_SALE_AND_VISA_VALE_6 = 6;
        public static final int SOLUTION_SALE_AND_VISA_VALE_12 = 12;
        public static final int QUERY = 13;
        public static final int TICKET = 14;
        public static final int[][] DEBIT_RANGES_NEW = {{200, 299}, {500, 599}};

        private HostFlow() {
        }
    }
}
