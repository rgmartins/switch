package com.guzula.pswitch.capture.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guzula.pswitch.registry.terminal.TerminalConfig;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PosServiceTest {

  @Test
  void sendsMockG0ToHsmConnection() {
    AtomicReference<String> connection = new AtomicReference<>();
    AtomicReference<byte[]> sentPayload = new AtomicReference<>();
    PosService service =
        new PosService(
            (connectionId, payload) -> {
              connection.set(connectionId);
              sentPayload.set(payload);
            },
            null,
            null,
            terminalId -> Optional.empty());

    service.sendComandoG0();

    String command = new String(sentPayload.get(), StandardCharsets.US_ASCII);
    assertEquals("HSM", connection.get());
    assertEquals(128, command.length());
    assertEquals("9876G0", command.substring(0, 6));
    assertEquals("0123456789ABCDEF", command.substring(93, 109));
  }

  @Test
  void receivesValidG1FromHsmConnection() {
    PosService service =
        new PosService((connectionId, payload) -> {}, null, null, terminalId -> Optional.empty());
    byte[] response = "9876G100160123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    service.handleInbound("HSM", response);
  }

  @Test
  void rejectsReceiveWithoutPendingG1() {
    PosService service =
        new PosService((connectionId, payload) -> {}, null, null, terminalId -> Optional.empty());

    assertThrows(IllegalStateException.class, service::receiveComandoG0);
  }

  @Test
  void populatesCanonicalFromTerminalRegistry() {
    TerminalConfig.Address terminalAddress =
        new TerminalConfig.Address(
            "Rua das Flores", "123", "Loja 1", "Centro", "Sao Paulo", "01001000", "SP", "BR");
    PosService service =
        new PosService(
            (connectionId, payload) -> {},
            null,
            null,
            terminalId ->
                Optional.of(
                    new TerminalConfig(
                        "mongo-id",
                        terminalId,
                        "Mercado Teste",
                        10169548130001L,
                        42L,
                        54321L,
                        9876L,
                        998877L,
                        665544L,
                        terminalAddress,
                        "j",
                        "10169548130001",
                        "solucao_pos",
                        true,
                        true,
                        false)));
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setTerminalId("01361475");

    service.populateTerminalData(canonical);

    CanonicalTransaction.Merchant merchant = canonical.getMerchant();
    assertEquals("Mercado Teste", merchant.getName());
    assertEquals("10169548130001", merchant.getMerchant());
    assertEquals("42", merchant.getStore());
    assertEquals("54321", merchant.getMerchantPhysical());
    assertEquals("9876", merchant.getStorePhysical());
    assertEquals("998877", merchant.getMerchantHeadquarters());
    assertEquals("665544", merchant.getStoreHeadquarters());
    assertEquals("j", merchant.getPerson());
    assertEquals("10169548130001", merchant.getCnpjOrCpf());

    CanonicalTransaction.Merchant.Address address = merchant.getAddress();
    assertEquals("Rua das Flores", address.getStreet());
    assertEquals("123", address.getNumber());
    assertEquals("Loja 1", address.getComplement());
    assertEquals("Centro", address.getNeighborhood());
    assertEquals("Sao Paulo", address.getCity());
    assertEquals("01001000", address.getZipCode());
    assertEquals("SP", address.getUf());
    assertEquals("BR", address.getCountry());

    assertEquals("solucao_pos", canonical.getEquipment().getMobilePaymentType());
    assertEquals(true, canonical.getEquipment().getDoesPreAuth());
    assertEquals(true, canonical.getEquipment().getDoesDcc());
    assertEquals(false, canonical.getEquipment().getTerminalBlocked());
  }
}
