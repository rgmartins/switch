package com.guzula.pswitch.brand.visa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.guzula.pswitch.brand.visa.packer.VisaPackerService;
import com.guzula.pswitch.brand.visa.parser.VisaParserService;
import com.guzula.pswitch.nucleo.NucleoService;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import com.guzula.pswitch.shared.iso8583.Iso8583Codec;
import com.guzula.pswitch.shared.port.OutboundPayloadSender;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisaServiceTest {

  @Test
  void authorizeSendsRequestAndHandleInboundCorrelatesPopulatesAndNotifiesNucleo() {
    Map<String, byte[]> sent = new HashMap<>();
    OutboundPayloadSender payloadSender =
        (connectionId, payload) -> sent.put(connectionId, payload);
    NucleoService nucleoService = mock(NucleoService.class);
    VisaService visaService =
        new VisaService(
            new VisaPackerService(), new VisaParserService(), payloadSender, nucleoService);

    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setNsu("1000");
    canonical.setTerminalId("00891592");
    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setCardNumber("4111111111111111");
    canonical.setCard(card);
    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setAmount(3600);
    canonical.setOperation(operation);
    CanonicalTransaction.Merchant merchant = new CanonicalTransaction.Merchant();
    merchant.setMerchant("10169548130001");
    canonical.setMerchant(merchant);

    visaService.authorize(canonical);
    assertNotNull(sent.get("VISA"));

    byte[] response = approvedResponse("00891592", 1000, "AB12C3");
    visaService.handleInbound("VISA", response);

    CanonicalTransaction.Response result = canonical.getResponse();
    assertNotNull(result);
    assertEquals("00", result.getResponseCode());
    assertEquals("AB12C3", result.getAuthorizationCode());
    verify(nucleoService).handleResponse(canonical);
  }

  @Test
  void handleInboundIgnoresResponseWithoutAPendingTransaction() {
    NucleoService nucleoService = mock(NucleoService.class);
    VisaService visaService =
        new VisaService(
            new VisaPackerService(),
            new VisaParserService(),
            (connectionId, payload) -> {},
            nucleoService);

    // Nenhum authorize() foi chamado antes — não há nada pendente pra correlacionar.
    visaService.handleInbound("VISA", approvedResponse("00000000", 1, "ZZZZZZ"));

    verify(nucleoService, org.mockito.Mockito.never())
        .handleResponse(org.mockito.ArgumentMatchers.any());
  }

  private byte[] approvedResponse(String terminalId, long nsu, String authorizationCode) {
    byte[] bitmap = new byte[8];
    for (int de : new int[] {3, 4, 11, 38, 39, 41}) {
      setBit(bitmap, de);
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(new byte[22]); // cabeçalho (não usado na correlação)
    out.writeBytes(Iso8583Codec.encodeBCD("0110"));
    out.writeBytes(bitmap);
    out.writeBytes(Iso8583Codec.encodeBCD("000000")); // DE3
    out.writeBytes(Iso8583Codec.encodeBCD(3600L, 6)); // DE4
    out.writeBytes(Iso8583Codec.encodeBCD(nsu, 3)); // DE11
    out.writeBytes(Iso8583Codec.encodeEBCDIC(authorizationCode, 6)); // DE38
    out.writeBytes(Iso8583Codec.encodeEBCDIC("00", 2)); // DE39
    out.writeBytes(Iso8583Codec.encodeEBCDIC(terminalId, 8)); // DE41
    return out.toByteArray();
  }

  private void setBit(byte[] bitmap, int de) {
    int byteIndex = (de - 1) / 8;
    int bitIndex = (de - 1) % 8;
    bitmap[byteIndex] |= (byte) (0x80 >> bitIndex);
  }
}
