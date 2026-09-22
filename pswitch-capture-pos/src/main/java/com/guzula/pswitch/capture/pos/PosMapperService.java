package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosTransaction;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/**
 * Referência: pos-mapper.service.ts (guzula-switch).
 * Converte a mensagem ISO parseada (parser) para o modelo canônico.
 *
 * TODO: mapear cardholderName (EMV 5F20) e cardBrand a partir do DE055.
 */
@Service
public class PosMapperService {

    public CanonicalTransaction toCanonical(PosMessage message) {
        PosTransaction transaction = PosTransaction.from(message);
        System.out.println(transaction);

        CanonicalTransaction canonical = new CanonicalTransaction();
        canonical.setTerminalId(transaction.de041TerminalId());
        canonical.setEquipment(equipment(transaction));
        canonical.setCard(card(transaction));
        canonical.setOperation(operation(transaction));
        return canonical;
    }

    private CanonicalTransaction.Equipment equipment(PosTransaction transaction) {
        CanonicalTransaction.Equipment equipment = new CanonicalTransaction.Equipment();
        equipment.setTerminalId(transaction.de041TerminalId());
        return equipment;
    }

    private CanonicalTransaction.Card card(PosTransaction transaction) {
        CanonicalTransaction.Card card = new CanonicalTransaction.Card();
        card.setCardSequenceNumber(transaction.de023CardSequenceNumber());
        return card;
    }

    private CanonicalTransaction.Operation operation(PosTransaction transaction) {
        CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
        if (transaction.de004AmountTransaction() != null) {
            operation.setAmount(transaction.de004AmountTransaction().movePointRight(2).longValueExact());
        }
        operation.setCurrencyCode(transaction.de049CurrencyCode());
        return operation;
    }
}
