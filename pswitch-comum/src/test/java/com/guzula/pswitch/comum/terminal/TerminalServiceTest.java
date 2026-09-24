package com.guzula.pswitch.comum.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.guzula.pswitch.comum.ComumService;
import com.guzula.pswitch.comum.bin.BinService;
import com.guzula.pswitch.comum.keyblock.KeyblockService;
import com.guzula.pswitch.external.hsm.HsmService;
import com.guzula.pswitch.registry.bin.BinConfig;
import com.guzula.pswitch.registry.keyblock.KeyblockConfig;
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
    KeyblockService keyblockService =
        new KeyblockService(
            keyblockId ->
                Optional.of(
                    new KeyblockConfig(
                        "mongo-key-id",
                        keyblockId,
                        "2026-01-01T00:00:00Z",
                        "0123456789ABCDEFFEDCBA9876543210",
                        "",
                        "")));
    HsmService hsmService = mock(HsmService.class);
    BinConfig bin =
        new BinConfig(
            "mongo-bin-id",
            "4000000000000000",
            "4999999999999999",
            "Visa",
            1,
            "BR",
            1,
            1,
            1,
            false,
            true,
            false,
            false,
            false,
            "credit");
    ComumService comumService =
        new ComumService(
            terminalService, new BinService(pan -> Optional.of(bin)), keyblockService, hsmService);
    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setTerminalId("01361475");
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4000000000000002");
    canonical.setCard(card);
    CanonicalTransaction.Security.Ksn ksn = new CanonicalTransaction.Security.Ksn();
    ksn.setBdkIndicator("fffff17001");
    CanonicalTransaction.Security security = new CanonicalTransaction.Security();
    security.setKsn(ksn);
    canonical.setSecurity(security);

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
