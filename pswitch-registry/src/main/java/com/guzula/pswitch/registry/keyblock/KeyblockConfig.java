package com.guzula.pswitch.registry.keyblock;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Chave de teste armazenada na coleção MongoDB {@code Keyblock}. */
@Document(collection = "Keyblock")
public record KeyblockConfig(
    @Id String id,
    String key,
    String dateTime1,
    String keyblock1,
    String dateTime2,
    String keyblock2) {}
