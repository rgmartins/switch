package com.guzula.pswitch.registry.terminal;

import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * Consulta os terminais na coleção MongoDB {@code Terminal}. Referência:
 * terminal-registry.service.ts (guzula-switch).
 */
@Service
public class TerminalRegistryService implements TerminalRegistry {

  private final MongoTemplate mongoTemplate;

  public TerminalRegistryService(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<TerminalConfig> findByTerminalId(String terminalId) {
    if (terminalId == null || terminalId.isBlank()) {
      return Optional.empty();
    }

    Query query = Query.query(Criteria.where("key").is(terminalId));
    return Optional.ofNullable(mongoTemplate.findOne(query, TerminalConfig.class));
  }
}
