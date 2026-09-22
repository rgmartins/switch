package com.guzula.pswitch.shared;

/**
 * Constantes compartilhadas entre módulos.
 * Referência: src/shared/constants.ts (guzula-switch).
 *
 * TODO: portar os demais grupos de constants.ts conforme forem sendo necessários
 * (MCC, CARD_PRODUCT, CURRENCY_CODE, VISA_*, WHO_RESPONDED, MESSAGE_DESCRIPTION_SOURCE...).
 */
public final class SwitchConstants {

    private SwitchConstants() {
    }

    public static final class TypePersona {
        public static final String PHYSICAL_PERSON = "f";
        public static final String LEGAL_PERSON = "j";

        private TypePersona() {
        }
    }

    public static final class TypeEquipment {
        public static final String TEF41 = "1";
        public static final String TEF2000 = "2";
        public static final String POS40 = "3";

        private TypeEquipment() {
        }
    }

    /** DE 22 posição 1 (forma de entrada). */
    public static final class EntryMode {
        public static final String CHIP = "0";
        public static final String CONTACTLESS_CHIP = "9";
        public static final String TRACK_2 = "3";
        public static final String TRACK_1 = "7";
        public static final String CONTACTLESS_TRACK = "8";
        public static final String TYPED = "4";

        private EntryMode() {
        }
    }

    /** DE 22 posição 0 (pinpad físico). */
    public static final class PinpadPhysical {
        public static final String NO_PINPAD = "1";
        public static final String WITHOUT_CHIP_READER = "2";
        public static final String WITH_CHIP_NO_SAM = "3";
        public static final String WITH_CHIP_WITH_SAM = "4";
        public static final String NOT_HOMOLOGATED = "5";
        public static final String WITH_CHIP_CONTACTLESS_NO_SAM = "6";
        public static final String WITH_CHIP_CONTACTLESS_WITH_SAM = "7";

        private PinpadPhysical() {
        }
    }

    public static final class InputMethod {
        public static final String CHIP = "chip";
        public static final String TRACK_1 = "tr1";
        public static final String TRACK_2 = "tr2";
        public static final String TYPED = "digitada";

        private InputMethod() {
        }
    }

    public static final class InteractionPartner {
        public static final String POS = "pos";
        public static final String VISA = "visa";
        public static final String MASTERCARD = "mastercard";
        public static final String AMEX = "amex";
        public static final String HSM = "hsm";
        public static final String ANTIFRAUD = "antifraud";
        public static final String PLANET = "planet";
        public static final String TARIFAS = "tarifas";
        public static final String PRE_AUTH = "pre-auth";
        public static final String HISTORICO = "historico";

        private InteractionPartner() {
        }
    }

    public static final class InteractionDirection {
        public static final String REQUEST = "request";
        public static final String RESPONSE = "response";

        private InteractionDirection() {
        }
    }

    public static final class Channel {
        public static final String POS = "POS";
        public static final String TEF = "TEF";

        private Channel() {
        }
    }

    public static final class Brand {
        public static final int UNKNOWN = 0;
        public static final int VISA = 1;
        public static final int MASTERCARD = 2;
        public static final int AMEX = 3;
        public static final int ELO = 7;

        private Brand() {
        }
    }

    public static final class Trn {
        public static final String MODALITY_CREDIT = "credit";
        public static final String MODALITY_DEBIT = "debit";

        private Trn() {
        }
    }

    public static final class TransactionType {
        public static final String CREDIT = "100";
        public static final String INSTALLMENT_ISSUER = "101";
        public static final String INSTALLMENT_MERCHANT = "102";
        public static final String REVERSAL = "103";
        public static final String DEBIT = "117";
        public static final String CREDIT_SIMULATION_ON_CREDIT = "129";
        public static final String UNDO = "999";

        private TransactionType() {
        }
    }
}
