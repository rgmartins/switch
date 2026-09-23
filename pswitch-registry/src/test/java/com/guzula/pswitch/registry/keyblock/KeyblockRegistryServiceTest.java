package com.guzula.pswitch.registry.keyblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class KeyblockRegistryServiceTest {

  @Test
  void findsKeyblockByKey() {
    MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    KeyblockConfig keyblock =
        new KeyblockConfig(
            "mongo-id",
            "fffff17001",
            "2026-01-01T00:00:00Z",
            "0123456789ABCDEFFEDCBA9876543210",
            "",
            "");
    when(mongoTemplate.findOne(
            org.mockito.ArgumentMatchers.any(Query.class), eq(KeyblockConfig.class)))
        .thenReturn(keyblock);
    KeyblockRegistryService service = new KeyblockRegistryService(mongoTemplate);

    assertEquals(keyblock, service.findByKeyblockId("fffff17001").orElseThrow());

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).findOne(query.capture(), eq(KeyblockConfig.class));
    assertEquals(new Document("key", "fffff17001"), query.getValue().getQueryObject());
  }
}
