package com.guzula.pswitch.external.hsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HsmServiceTest {

  private static final String SOURCE_KEY = "0123456789ABCDEFFEDCBA9876543210";

  @Test
  void sendsSeWaitsForSfAndPopulatesCard() {
    AtomicReference<byte[]> requestSent = new AtomicReference<>();
    AtomicReference<HsmService> serviceReference = new AtomicReference<>();
    String track = "4123456789012349=29122010000000000000";
    HsmService service =
        new HsmService(
            new HsmRequestManager(
                (connectionId, payload) -> {
                  assertEquals("HSM", connectionId);
                  requestSent.set(payload.clone());
                  String header = new String(payload, 0, 4, StandardCharsets.US_ASCII);
                  byte[] response =
                      (header + "SF00" + "%05d".formatted(track.length()) + track)
                          .getBytes(StandardCharsets.US_ASCII);
                  serviceReference.get().handleInbound(connectionId, response);
                },
                Duration.ofSeconds(1)),
            new HsmSeProtocol());
    serviceReference.set(service);
    CanonicalTransaction canonical = canonical();

    service.decryptCardData(canonical, SOURCE_KEY);

    byte[] request = requestSent.get();
    String requestPrefix = new String(request, 0, 69, StandardCharsets.US_ASCII);
    assertTrue(requestPrefix.startsWith("0000SE01" + SOURCE_KEY + "505FFFFF1700168A060069C3"));
    assertTrue(requestPrefix.endsWith("00006"));
    assertEquals(";99%01", new String(request, request.length - 6, 6, StandardCharsets.US_ASCII));
    assertEquals(track, canonical.getCard().getTrack());
    assertEquals("4123456789012349", canonical.getCard().getCardNumber());
    assertEquals("2912", canonical.getCard().getExpirationDate());
    assertEquals("201", canonical.getCard().getServiceCode());
  }

  @Test
  void failsWhenSfDoesNotArriveBeforeTimeout() {
    HsmService service =
        new HsmService(
            new HsmRequestManager((connectionId, payload) -> {}, Duration.ofMillis(20)),
            new HsmSeProtocol());

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class, () -> service.decryptCardData(canonical(), SOURCE_KEY));

    assertTrue(error.getMessage().contains("Timeout aguardando resposta SF"));
  }

  private CanonicalTransaction canonical() {
    CanonicalTransaction.Security.Ksn ksn = new CanonicalTransaction.Security.Ksn();
    ksn.setBdkIndicator("FFFFF17001");
    ksn.setPinPadIndicator("68A06");
    ksn.setTransactionCounter("0069C");
    CanonicalTransaction.Security security = new CanonicalTransaction.Security();
    security.setKsn(ksn);
    security.setEncryptedCardData("A1B2C3D4E5F6");
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setSecurity(security);
    canonical.setCard(new CanonicalTransaction.Card());
    return canonical;
  }
}
