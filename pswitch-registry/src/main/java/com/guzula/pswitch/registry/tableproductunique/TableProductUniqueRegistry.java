package com.guzula.pswitch.registry.tableproductunique;

import java.util.Optional;

/**
 * Contrato do registry de produto único. Referência: table-product-unique.service.ts
 * (guzula-switch).
 */
public interface TableProductUniqueRegistry {

  Optional<TableProductUniqueConfig> findByKey(String key);
}
