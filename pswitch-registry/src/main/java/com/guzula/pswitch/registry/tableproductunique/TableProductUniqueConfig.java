package com.guzula.pswitch.registry.tableproductunique;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Conversão de produto único (agnóstico de bandeira) para o produto específico de liquidação de
 * cada bandeira. Referência: table-product-unique.service.ts (guzula-switch), coleção MongoDB
 * {@code TableProductUnique}.
 */
@Document(collection = "TableProductUnique")
public record TableProductUniqueConfig(
    @Id String id,
    String key,
    String description,
    Integer product,
    Integer productSub,
    List<Brand> brands) {

  public record Brand(Integer brand, Product product) {}

  public record Product(Integer product, Integer productSub, Integer cardType) {}
}
