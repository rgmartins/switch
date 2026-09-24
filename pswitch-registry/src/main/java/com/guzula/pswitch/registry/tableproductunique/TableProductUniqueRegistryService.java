package com.guzula.pswitch.registry.tableproductunique;

import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/** Consulta o produto único na coleção MongoDB {@code TableProductUnique}. */
@Service
public class TableProductUniqueRegistryService implements TableProductUniqueRegistry {

  private final MongoTemplate mongoTemplate;

  public TableProductUniqueRegistryService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<TableProductUniqueConfig> findByKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }

    Query query = Query.query(Criteria.where("key").is(key));
    return Optional.ofNullable(mongoTemplate.findOne(query, TableProductUniqueConfig.class));
  }
}
