package com.guzula.pswitch.comum.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.registry.terminal.TerminalConfig;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TerminalServiceTest {

  @Test
  void comumServicePopulatesCanonicalFromTerminalRegistry() {
    TerminalConfig.Address terminalAddress =
        new TerminalConfig.Address(
            "Rua das Flores", "123", "Loja 1", "Centro", "Sao Paulo", "01001000", "SP", "BR");
    TerminalService terminalService =
        new TerminalService(
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
    ComumService comumService = new ComumService(terminalService);
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setTerminalId("01361475");

    assertSame(canonical, comumService.process(canonical));

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
