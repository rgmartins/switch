package com.guzula.pswitch.comum.terminal;

import com.guzula.pswitch.registry.terminal.TerminalConfig;
import com.guzula.pswitch.registry.terminal.TerminalRegistry;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import org.springframework.stereotype.Service;

/** Popula o canônico com os dados do terminal cadastrado. */
@Service
public class TerminalService {

  private final TerminalRegistry terminalRegistry;

  public TerminalService(TerminalRegistry terminalRegistry) {
    this.terminalRegistry = terminalRegistry;
  }

  public void populate(CanonicalTransaction canonical) {
    TerminalConfig terminal =
        terminalRegistry
            .findByTerminalId(canonical.getTerminalId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Terminal não cadastrado: " + canonical.getTerminalId()));

    TerminalConfig.Address source = terminal.address();
    if (source == null) {
      throw new IllegalStateException(
          "Terminal sem endereço cadastrado: " + canonical.getTerminalId());
    }

    CanonicalTransaction.Merchant merchant = canonical.getMerchant();
    if (merchant == null) {
      merchant = new CanonicalTransaction.Merchant();
      canonical.setMerchant(merchant);
    }

    merchant.setName(terminal.merchantName());
    if (merchant.getMerchant() == null || merchant.getMerchant().isBlank()) {
      merchant.setMerchant(asString(terminal.merchantId()));
    }
    if (merchant.getStore() == null || merchant.getStore().isBlank()) {
      merchant.setStore(asString(terminal.storeId()));
    }
    merchant.setMerchantPhysical(asString(terminal.merchantPhysical()));
    merchant.setStorePhysical(asString(terminal.storePhysical()));
    merchant.setMerchantHeadquarters(asString(terminal.merchantHeadquarters()));
    merchant.setStoreHeadquarters(asString(terminal.storeHeadquarters()));
    merchant.setPerson(terminal.person());
    merchant.setCnpjOrCpf(terminal.cnpjOrCpf());
    merchant.setAddress(address(source));

    CanonicalTransaction.Equipment equipment = canonical.getEquipment();
    if (equipment == null) {
      equipment = new CanonicalTransaction.Equipment();
      canonical.setEquipment(equipment);
    }
    equipment.setMobilePaymentType(terminal.mobilePaymentType());
    equipment.setDoesPreAuth(terminal.doesPreAuth());
    equipment.setDoesDcc(terminal.doesDcc());
    equipment.setTerminalBlocked(terminal.blocked());
  }

  private CanonicalTransaction.Merchant.Address address(TerminalConfig.Address source) {
    CanonicalTransaction.Merchant.Address address = new CanonicalTransaction.Merchant.Address();
    address.setStreet(source.street());
    address.setNumber(source.number());
    address.setComplement(source.complement());
    address.setNeighborhood(source.neighborhood());
    address.setCity(source.city());
    address.setZipCode(source.zipCode());
    address.setUf(source.uf());
    address.setCountry(source.country());
    return address;
  }

  private String asString(Long value) {
    return value == null ? null : value.toString();
  }
}
