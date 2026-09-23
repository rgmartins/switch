package com.guzula.pswitch.registry.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class TerminalRegistryServiceTest {

  @Test
  void findsTerminalByKey() {
    MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    TerminalConfig terminal = new TerminalConfig("mongo-id", "01361475", null);
    when(mongoTemplate.findOne(
            org.mockito.ArgumentMatchers.any(Query.class), eq(TerminalConfig.class)))
        .thenReturn(terminal);
    TerminalRegistryService service = new TerminalRegistryService(mongoTemplate);

    assertEquals(terminal, service.findByTerminalId("01361475").orElseThrow());

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).findOne(query.capture(), eq(TerminalConfig.class));
    assertEquals(new Document("key", "01361475"), query.getValue().getQueryObject());
  }

  @Test
  void ignoresBlankTerminalId() {
    MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    TerminalRegistryService service = new TerminalRegistryService(mongoTemplate);

    assertFalse(service.findByTerminalId(" ").isPresent());
    verifyNoInteractions(mongoTemplate);
  }
}
