package com.guzula.pswitch.shared.domain;

/**
 * Modelo canônico de transação — contrato central lido/escrito por todos os módulos.
 * Referência: src/shared/domain/canonical-transaction.ts (guzula-switch).
 *
 * TODO: portar os demais campos do canonical-transaction.ts original.
 */
public class CanonicalTransaction {

    private String terminalId;
    private Equipment equipment;
    private Card card;
    private Operation operation;

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public static class Equipment {
        private String terminalId; // DE 41

        public String getTerminalId() {
            return terminalId;
        }

        public void setTerminalId(String terminalId) {
            this.terminalId = terminalId;
        }
    }

    public static class Card {
        private String cardSequenceNumber; // DE 23
        private String cardholderName; // EMV tag 5F20
        private CardBrand cardBrand;

        public String getCardSequenceNumber() {
            return cardSequenceNumber;
        }

        public void setCardSequenceNumber(String cardSequenceNumber) {
            this.cardSequenceNumber = cardSequenceNumber;
        }

        public String getCardholderName() {
            return cardholderName;
        }

        public void setCardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
        }

        public CardBrand getCardBrand() {
            return cardBrand;
        }

        public void setCardBrand(CardBrand cardBrand) {
            this.cardBrand = cardBrand;
        }
    }

    public static class CardBrand {
        private String authorization; // usado pelo NucleoService para rotear

        public String getAuthorization() {
            return authorization;
        }

        public void setAuthorization(String authorization) {
            this.authorization = authorization;
        }
    }

    public static class Operation {
        private double amount; // DE 4 — TODO ver TODO.md do guzula-switch: migrar para centavos (long)
        private String currencyCode; // DE 49

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
        }
    }
}
