package com.guzula.pswitch.nucleo.correlation;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Correlaciona um pedido enviado a um {@link com.guzula.pswitch.nucleo.BrandHandler BrandHandler}
 * (ex.: autorização à Visa) com a resposta que chega depois, de forma assíncrona — via Redis, para
 * que a resposta possa ser processada por uma réplica diferente da que enviou o pedido. Também
 * detecta pedidos nunca respondidos dentro de um prazo ({@link #sweepExpired}).
 *
 * <p>Extraído do {@code VisaService} para ser reaproveitado por qualquer outro adaptador de
 * bandeira que precise do mesmo padrão de correlação com timeout (ver
 * docs/arquitetura/topologia-implantacao.md).
 *
 * @param <T> tipo do valor pendente (ex.: {@code CanonicalTransaction})
 */
public final class RedisPendingCorrelator<T> {

  private final StringRedisTemplate redisTemplate;
  private final String keyPrefix;
  private final JavaType entryType;
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public RedisPendingCorrelator(
      StringRedisTemplate redisTemplate, String keyPrefix, Class<T> valueType) {
    this.redisTemplate = redisTemplate;
    this.keyPrefix = keyPrefix;
    this.entryType = objectMapper.getTypeFactory().constructParametricType(Entry.class, valueType);
  }

  /** Guarda {@code value} como pendente sob {@code correlationKey}, expirando em {@code ttl}. */
  public void store(String correlationKey, T value, Duration ttl) {
    Entry<T> entry = new Entry<>(value, Instant.now());
    redisTemplate.opsForValue().set(keyPrefix + correlationKey, writeJson(entry), ttl);
  }

  /**
   * Reivindica de forma atômica o valor pendente sob {@code correlationKey}, removendo-o. Devolve
   * {@code null} se não houver nada pendente para essa chave (já reivindicado ou nunca existiu).
   */
  public T claim(String correlationKey) {
    Entry<T> entry = claimByRedisKey(keyPrefix + correlationKey);
    return entry == null ? null : entry.value();
  }

  /**
   * Varre todos os valores pendentes deste correlacionador e, para cada um parado há mais de {@code
   * timeout}, reivindica-o atomicamente e chama {@code onExpired(correlationKey, value)}.
   *
   * <p>Espia (leitura simples) antes de reivindicar (leitura + remoção atômica) para não roubar uma
   * entrada que {@link #claim} já tenha pego nesse meio tempo — se isso acontecer, a reivindicação
   * aqui devolve {@code null} e essa entrada é ignorada.
   */
  public void sweepExpired(Duration timeout, BiConsumer<String, T> onExpired) {
    Instant now = Instant.now();
    ScanOptions scanOptions = ScanOptions.scanOptions().match(keyPrefix + "*").count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
      while (cursor.hasNext()) {
        String redisKey = cursor.next();
        Entry<T> candidate = peek(redisKey);
        if (candidate == null || Duration.between(candidate.sentAt(), now).compareTo(timeout) < 0) {
          continue;
        }
        Entry<T> claimed = claimByRedisKey(redisKey);
        if (claimed != null) {
          onExpired.accept(redisKey.substring(keyPrefix.length()), claimed.value());
        }
      }
    }
  }

  private Entry<T> peek(String redisKey) {
    String json = redisTemplate.opsForValue().get(redisKey);
    return json == null ? null : readJson(json);
  }

  private Entry<T> claimByRedisKey(String redisKey) {
    String json = redisTemplate.opsForValue().getAndDelete(redisKey);
    return json == null ? null : readJson(json);
  }

  private String writeJson(Entry<T> entry) {
    try {
      return objectMapper.writeValueAsString(entry);
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao serializar valor pendente", exception);
    }
  }

  private Entry<T> readJson(String json) {
    try {
      return objectMapper.readValue(json, entryType);
    } catch (Exception exception) {
      throw new IllegalStateException("Falha ao desserializar valor pendente", exception);
    }
  }

  private record Entry<T>(T value, Instant sentAt) {}
}
