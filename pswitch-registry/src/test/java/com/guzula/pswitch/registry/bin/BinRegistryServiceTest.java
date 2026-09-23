package com.guzula.pswitch.registry.bin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class BinRegistryServiceTest {

  @Test
  void findsRangeContainingTheNormalizedPan() {
    MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    BinConfig bin =
        new BinConfig(
            "4158960000000000000",
            "4158960000000000000",
            "4158969999999999999",
            "Cartão de Teste Visa",
            999,
            "BR",
            1,
            1,
            1,
            false,
            true,
            false,
            false,
            false,
            "C");
    when(mongoTemplate.findOne(any(Query.class), eq(BinConfig.class))).thenReturn(bin);
    BinRegistryService service = new BinRegistryService(mongoTemplate);

    assertEquals(bin, service.findByPan("4158961234567890").orElseThrow());

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongoTemplate).findOne(query.capture(), eq(BinConfig.class));
    assertEquals(
        new Document("initialCardBin", new Document("$lte", "4158961234567890000"))
            .append("finalCardBin", new Document("$gte", "4158961234567890999")),
        query.getValue().getQueryObject());
  }
}
