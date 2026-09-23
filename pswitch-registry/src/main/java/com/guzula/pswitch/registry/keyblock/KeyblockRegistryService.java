package com.guzula.pswitch.registry.keyblock;

import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/** Consulta as chaves na coleção MongoDB {@code Keyblock}. */
@Service
public class KeyblockRegistryService implements KeyblockRegistry {

  private final MongoTemplate mongoTemplate;

  public KeyblockRegistryService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<KeyblockConfig> findByKeyblockId(String keyblockId) {
    if (keyblockId == null || keyblockId.isBlank()) {
      return Optional.empty();
    }

    Query query = Query.query(Criteria.where("key").is(keyblockId));
    return Optional.ofNullable(mongoTemplate.findOne(query, KeyblockConfig.class));
  }
}
