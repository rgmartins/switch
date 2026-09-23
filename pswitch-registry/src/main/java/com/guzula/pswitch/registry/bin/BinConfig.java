package com.guzula.pswitch.registry.bin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Cadastro de faixa de BIN com o mesmo formato usado pelo guzula-switch. */
@Document(collection = "Bin")
public record BinConfig(
    @Id String id,
    String initialCardBin,
    String finalCardBin,
    String description,
    Integer bank,
    String country,
    Integer cardBrandAuthorization,
    Integer cardBrandRegister,
    Integer cardBrandSettlement,
    Boolean isDebit,
    Boolean isCredit,
    Boolean isForeign,
    Boolean isCorporate,
    Boolean isDebitMaster,
    String product) {}
