package com.guzula.pswitch.registry.terminal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Cadastro de terminal armazenado na coleção MongoDB {@code Terminal}. */
@Document(collection = "Terminal")
public record TerminalConfig(
    @Id String id,
    String key,
    String merchantName,
    Long merchantId,
    Long storeId,
    Long merchantPhysical,
    Long storePhysical,
    Long merchantHeadquarters,
    Long storeHeadquarters,
    Address address,
    String person,
    String cnpjOrCpf,
    String mobilePaymentType,
    Boolean doesPreAuth,
    Boolean doesDcc,
    Boolean blocked) {

  public TerminalConfig(String id, String key, Address address) {
    this(
        id, key, null, null, null, null, null, null, null, address, null, null, null, null, null,
        null);
  }

  public record Address(
      String street,
      String number,
      String complement,
      String neighborhood,
      String city,
      String zipCode,
      String uf,
      String country) {}
}
