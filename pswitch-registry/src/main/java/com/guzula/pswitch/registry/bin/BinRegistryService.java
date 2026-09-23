package com.guzula.pswitch.registry.bin;

import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/** Consulta a faixa do cartão na coleção MongoDB {@code Bin}. */
@Service
public class BinRegistryService implements BinRegistry {

  private final MongoTemplate mongoTemplate;

  public BinRegistryService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<BinConfig> findByPan(String pan) {
    if (pan == null || !pan.matches("\\d{1,19}")) {
      return Optional.empty();
    }

    String binStart = pan + "0".repeat(19 - pan.length());
    String binEnd = pan + "9".repeat(19 - pan.length());
    Query query =
        Query.query(Criteria.where("initialCardBin").lte(binStart).and("finalCardBin").gte(binEnd));
    return Optional.ofNullable(mongoTemplate.findOne(query, BinConfig.class));
  }
}
