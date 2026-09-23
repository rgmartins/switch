package com.guzula.pswitch.comum.keyblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guzula.pswitch.registry.keyblock.KeyblockConfig;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class KeyblockServiceTest {

  @Test
  void loadsSourceKeyUsingBdkIndicator() {
    AtomicReference<String> requestedId = new AtomicReference<>();
    KeyblockConfig expected =
        new KeyblockConfig(
            "mongo-id",
            "fffff17001",
            "2026-01-01T00:00:00Z",
            "0123456789ABCDEFFEDCBA9876543210",
            "",
            "");
    KeyblockService service =
        new KeyblockService(
            keyblockId -> {
              requestedId.set(keyblockId);
              return Optional.of(expected);
            });
    CanonicalTransaction canonical = canonicalWithBdkIndicator("fffff17001");

    assertEquals(expected, service.getSourceKey(canonical));
    assertEquals("fffff17001", requestedId.get());
  }

  @Test
  void rejectsCanonicalWithoutBdkIndicator() {
    KeyblockService service = new KeyblockService(keyblockId -> Optional.empty());

    assertThrows(
        IllegalStateException.class, () -> service.getSourceKey(new CanonicalTransaction()));
  }

  private CanonicalTransaction canonicalWithBdkIndicator(String bdkIndicator) {
    CanonicalTransaction.Security.Ksn ksn = new CanonicalTransaction.Security.Ksn();
    ksn.setBdkIndicator(bdkIndicator);
    CanonicalTransaction.Security security = new CanonicalTransaction.Security();
    security.setKsn(ksn);
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setSecurity(security);
    return canonical;
  }
}
