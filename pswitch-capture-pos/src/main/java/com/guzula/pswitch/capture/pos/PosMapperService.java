package com.guzula.pswitch.capture.pos;

import com.guzula.pswitch.capture.pos.parser.PosMessage;
import com.guzula.pswitch.capture.pos.parser.PosTransaction;
import com.guzula.pswitch.capture.pos.parser.de55.De55Parser;
import com.guzula.pswitch.capture.pos.parser.de60.De60Parser;
import com.guzula.pswitch.capture.pos.parser.de61.De61Parser;
import com.guzula.pswitch.capture.pos.parser.de62.De62Parser;
import com.guzula.pswitch.shared.SwitchConstants;
import com.guzula.pswitch.shared.domain.Anotacao;
import com.guzula.pswitch.shared.domain.Anotacoes;
import com.guzula.pswitch.shared.domain.CanonicalTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Referência: pos-mapper.service.ts (guzula-switch), método buildCanonico. Converte a {@link
 * PosTransaction} (visão tipada da captura POS) para o modelo canônico. Todo campo do canônico é
 * montado exclusivamente a partir de PosTransaction — nunca lendo a {@link PosMessage} ou o payload
 * bruto diretamente.
 *
 * <p>TODO: DE061.03 (número de série do pinpad) e DE061.09 (transação original, usada no
 * cancelamento) ainda não têm subcampo decodificado em De61Parser; equipment.numberSeriePinpad e
 * originalTransaction ficam em branco até isso ser portado. TODO: portar interacoes/registros/time,
 * que dependem do dump da mensagem bruta e de marcações de tempo hoje não disponíveis nesta camada.
 */
@Service
public class PosMapperService {

  public CanonicalTransaction toCanonical(PosMessage message, String connectionId) {
    PosTransaction transaction = PosTransaction.from(message);

    CanonicalTransaction canonical = new CanonicalTransaction();
    canonical.setCommunication(communication(connectionId));
    canonical.setTerminalId(transaction.de041TerminalId());
    canonical.setNsu(nsu(transaction));
    canonical.setSecurity(security(transaction));
    canonical.setMerchant(merchant(transaction));
    canonical.setEquipment(equipment(transaction));
    canonical.setCard(card(transaction));
    canonical.setOperation(operation(transaction));
    canonical.setConfirmacaoEnviada(confirmacaoEnviada(transaction));
    canonical.setAnotacoes(anotacoes(transaction));
    canonical.setRegistros(List.of());
    canonical.setInteracoes(List.of());
    return canonical;
  }

  private String nsu(PosTransaction transaction) {
    return transaction.de011Nsu() == null ? null : transaction.de011Nsu().toString();
  }

  private CanonicalTransaction.Communication communication(String connectionId) {
    CanonicalTransaction.Communication communication = new CanonicalTransaction.Communication();
    communication.setSocketId(connectionId);
    communication.setChannel(SwitchConstants.Channel.POS);
    return communication;
  }

  private CanonicalTransaction.Security security(PosTransaction transaction) {
    CanonicalTransaction.Security security = new CanonicalTransaction.Security();
    security.setKsn(ksn(transaction));
    security.setEncryptedCardData(transaction.de035Track2Data());
    security.setPinBlock(transaction.de052PinBlock());
    return security;
  }

  private CanonicalTransaction.Security.Ksn ksn(PosTransaction transaction) {
    String ksnHex = trackEncryptionKsn(transaction);
    CanonicalTransaction.Security.Ksn ksn = new CanonicalTransaction.Security.Ksn();
    ksn.setBdkIndicator(substring(ksnHex, 0, 10));
    ksn.setPinPadIndicator(substring(ksnHex, 10, 15));
    ksn.setTransactionCounter(substring(ksnHex, 15, 20));
    return ksn;
  }

  private String trackEncryptionKsn(PosTransaction transaction) {
    De60Parser.Data de60 = transaction.de060AdditionalPosInformation();
    if (de60 == null) {
      return null;
    }
    De60Parser.Subfield subfield12 = de60.subfields().get("12");
    if (subfield12 != null
        && subfield12.details() instanceof De60Parser.TrackEncryptionData details) {
      return details.ksn();
    }
    return null;
  }

  private String substring(String value, int start, int end) {
    if (value == null || value.length() < end) {
      return null;
    }
    return value.substring(start, end);
  }

  private CanonicalTransaction.Merchant merchant(PosTransaction transaction) {
    String merchantId =
        transaction.de042MerchantId() == null ? "" : transaction.de042MerchantId().toString();
    String merchantLocation =
        transaction.de043MerchantLocation() == null
            ? ""
            : transaction.de043MerchantLocation().toString();

    CanonicalTransaction.Merchant merchant = new CanonicalTransaction.Merchant();
    merchant.setName("");
    merchant.setMerchant(merchantCode(merchantId));
    merchant.setStore(storeCode(merchantId));
    merchant.setMerchantPhysical(merchantCode(merchantLocation));
    merchant.setStorePhysical(storeCode(merchantLocation));
    merchant.setMerchantHeadquarters("0");
    merchant.setStoreHeadquarters("0");
    merchant.setAddress(new CanonicalTransaction.Merchant.Address());
    merchant.setMerchantCategoryCode(transaction.de018Mcc() == null ? "" : transaction.de018Mcc());
    merchant.setPerson(SwitchConstants.TypePersona.LEGAL_PERSON);
    merchant.setCnpjOrCpf("");
    return merchant;
  }

  /** DE 42/43: os 4 últimos dígitos são a loja (lógica/física); o restante é o estabelecimento. */
  private String merchantCode(String value) {
    if (value.length() <= 4) {
      return "0";
    }
    return value.substring(0, value.length() - 4);
  }

  private String storeCode(String value) {
    if (value.isEmpty()) {
      return "0";
    }
    if (value.length() <= 4) {
      return value;
    }
    return value.substring(value.length() - 4);
  }

  private CanonicalTransaction.Equipment equipment(PosTransaction transaction) {
    String posEntryMode = transaction.de022PosEntryMode();
    String pinpadFisico = charAt(posEntryMode, 0);

    CanonicalTransaction.Equipment equipment = new CanonicalTransaction.Equipment();
    equipment.setMobilePaymentType("");
    equipment.setType(SwitchConstants.TypeEquipment.POS40);
    equipment.setTerminalId(transaction.de041TerminalId());
    equipment.setTerminalOrigin("");
    equipment.setIdSoftware(softwareId(transaction));
    equipment.setNumberSeriePos(posSerialNumber(transaction));
    equipment.setNumberSeriePinpad(""); // DE061.03 ainda não é decodificado por De61Parser
    equipment.setDateTimeLocal(
        transaction.de012TimeLocalTransaction() == null
            ? ""
            : transaction.de012TimeLocalTransaction().raw());
    equipment.setDoesPreAuth(false);
    equipment.setPinPad(pinPad(pinpadFisico));
    equipment.setDoesDcc(false);
    equipment.setTerminalBlocked(false);
    return equipment;
  }

  private CanonicalTransaction.Equipment.PinPad pinPad(String pinpadFisico) {
    CanonicalTransaction.Equipment.PinPad pinPad = new CanonicalTransaction.Equipment.PinPad();
    pinPad.setHasPinpad(!pinpadFisico.isEmpty());
    pinPad.setHasPinpadChip(
        Set.of(
                SwitchConstants.PinpadPhysical.WITH_CHIP_NO_SAM,
                SwitchConstants.PinpadPhysical.WITH_CHIP_WITH_SAM,
                SwitchConstants.PinpadPhysical.WITH_CHIP_CONTACTLESS_NO_SAM,
                SwitchConstants.PinpadPhysical.WITH_CHIP_CONTACTLESS_WITH_SAM)
            .contains(pinpadFisico));
    return pinPad;
  }

  private String softwareId(PosTransaction transaction) {
    De61Parser.Data de61 = transaction.de061PosPrivateData();
    if (de61 == null) {
      return "";
    }
    De61Parser.Subfield sub01 = de61.subfields().get("01");
    if (sub01 != null && sub01.details() instanceof De61Parser.SoftwareIdentification details) {
      return details.softwareId();
    }
    return "";
  }

  private String posSerialNumber(PosTransaction transaction) {
    De61Parser.Data de61 = transaction.de061PosPrivateData();
    if (de61 == null) {
      return "";
    }
    De61Parser.Subfield sub02 = de61.subfields().get("02");
    if (sub02 != null && sub02.details() instanceof De61Parser.PosSerialNumber details) {
      return details.serialNumber();
    }
    return "";
  }

  private CanonicalTransaction.Card card(PosTransaction transaction) {
    String formaEntrada = charAt(transaction.de022PosEntryMode(), 1);
    boolean isContactless =
        SwitchConstants.EntryMode.CONTACTLESS_TRACK.equals(formaEntrada)
            || SwitchConstants.EntryMode.CONTACTLESS_CHIP.equals(formaEntrada);
    Map<String, String> emvTags = emvTags(transaction);

    CanonicalTransaction.Card card = new CanonicalTransaction.Card();
    card.setInputMethod(inputMethod(formaEntrada));
    card.setIsContactless(isContactless);
    card.setIsTapOnPhone(false);
    card.setCardNumber(""); // DE 2 (PAN) não existe no schema deste canal
    card.setExpirationDate("");
    card.setCvv(cvv());
    card.setCardBrand(cardBrand());
    card.setIsForeign(false);
    card.setIsCorporate(false);
    card.setIsDebit(false);
    card.setIsCredit(false);
    card.setIsDebitMaster(false);
    card.setProduct("");
    card.setCountry("0");
    card.setTrack(transaction.de035Track2Data());
    card.setServiceCode("");
    card.setCardSequenceNumber(transaction.de023CardSequenceNumber());
    card.setCardholderName(emvTags.getOrDefault("5f20", ""));
    card.setEmv(emv(emvTags));
    return card;
  }

  /** DE 22: posição 0 = pinpad físico, posição 1 = forma de entrada (de22.parser.ts). */
  private String charAt(String value, int index) {
    if (value == null || value.length() <= index) {
      return "";
    }
    return String.valueOf(value.charAt(index));
  }

  private String inputMethod(String formaEntrada) {
    if (SwitchConstants.EntryMode.CHIP.equals(formaEntrada)
        || SwitchConstants.EntryMode.CONTACTLESS_CHIP.equals(formaEntrada)) {
      return SwitchConstants.InputMethod.CHIP;
    }
    if (SwitchConstants.EntryMode.TRACK_2.equals(formaEntrada)
        || SwitchConstants.EntryMode.CONTACTLESS_TRACK.equals(formaEntrada)) {
      return SwitchConstants.InputMethod.TRACK_2;
    }
    if (SwitchConstants.EntryMode.TRACK_1.equals(formaEntrada)) {
      return SwitchConstants.InputMethod.TRACK_1;
    }
    return SwitchConstants.InputMethod.TYPED;
  }

  private CanonicalTransaction.Card.Cvv cvv() {
    CanonicalTransaction.Card.Cvv cvv = new CanonicalTransaction.Card.Cvv();
    cvv.setCvvIndicator(0);
    cvv.setCvvValue("");
    return cvv;
  }

  private CanonicalTransaction.Card.CardBrand cardBrand() {
    CanonicalTransaction.Card.CardBrand cardBrand = new CanonicalTransaction.Card.CardBrand();
    cardBrand.setAuthorization(String.valueOf(SwitchConstants.Brand.UNKNOWN));
    cardBrand.setRegister(String.valueOf(SwitchConstants.Brand.UNKNOWN));
    cardBrand.setSettlement(String.valueOf(SwitchConstants.Brand.UNKNOWN));
    cardBrand.setDoesDcc(false);
    cardBrand.setDoesPreAuth(false);
    return cardBrand;
  }

  private Map<String, String> emvTags(PosTransaction transaction) {
    De55Parser.Data de55 = transaction.de055IccData();
    if (de55 == null) {
      return Map.of();
    }
    De55Parser.Subfield sub07 = de55.subfields().get("07");
    if (sub07 != null && sub07.details() instanceof De55Parser.EmvData details) {
      return details.tags();
    }
    return Map.of();
  }

  private CanonicalTransaction.Emv emv(Map<String, String> tags) {
    List<CanonicalTransaction.EmvTag> emvTags = new ArrayList<>();
    tags.forEach(
        (tag, value) -> {
          CanonicalTransaction.EmvTag emvTag = new CanonicalTransaction.EmvTag();
          emvTag.setTag(tag);
          emvTag.setSize(value.length() / 2);
          emvTag.setValue(value);
          emvTags.add(emvTag);
        });
    CanonicalTransaction.Emv emv = new CanonicalTransaction.Emv();
    emv.setTags(emvTags);
    return emv;
  }

  private CanonicalTransaction.Operation operation(PosTransaction transaction) {
    int hostFlow = parseIntOrZero(transaction.de024HostFlow());
    String processingCode =
        transaction.de003ProcessingCode() == null ? "" : transaction.de003ProcessingCode();
    String mti = transaction.mti() == null ? "" : transaction.mti();

    boolean isDebitFlow = isDebitFlow(hostFlow);
    boolean isCreditSimulation =
        PosConstants.ProcessingCode.NAO_SEI_650000.equals(processingCode)
            || PosConstants.ProcessingCode.NAO_SEI_660000.equals(processingCode);

    String transactionType;
    if (isCreditSimulation) {
      transactionType = SwitchConstants.TransactionType.CREDIT_SIMULATION_ON_CREDIT;
    } else if (PosConstants.Mti.REVERSAL.equals(mti)) {
      transactionType = SwitchConstants.TransactionType.REVERSAL;
    } else if (isDebitFlow) {
      transactionType = SwitchConstants.TransactionType.DEBIT;
    } else {
      transactionType = SwitchConstants.TransactionType.CREDIT;
    }

    CanonicalTransaction.Operation operation = new CanonicalTransaction.Operation();
    operation.setTransactionModality(
        isDebitFlow ? SwitchConstants.Trn.MODALITY_DEBIT : SwitchConstants.Trn.MODALITY_CREDIT);
    operation.setTransactionType(transactionType);
    operation.setTransactionSubtype("");
    operation.setInstallmentPlan(installmentPlan(transaction));
    operation.setServiceFee(0L);
    operation.setBoardingFee(0L);
    operation.setEntryValue(0L);
    if (transaction.de004AmountTransaction() != null) {
      operation.setAmount(transaction.de004AmountTransaction().movePointRight(2).longValueExact());
    }
    operation.setCurrencyCode(transaction.de049CurrencyCode());
    operation.setNsu(nsu(transaction));
    operation.setKeyUnique("0");
    operation.setDateTime(
        transaction.de012TimeLocalTransaction() == null
            ? ""
            : transaction.de012TimeLocalTransaction().raw());
    operation.setHostFlow(transaction.de024HostFlow());
    operation.setDigitalWallet("");
    operation.setProduct(product(transaction));
    operation.setIsPreAuth(hostFlow == PosConstants.HostFlow.PRE_AUTHORIZATION);
    operation.setIsUndo(PosConstants.Mti.UNDO.equals(mti));
    operation.setIsRecurringPayment(false);
    operation.setCdc(cdc(hostFlow, processingCode));
    operation.setPrivateData(privateData());
    return operation;
  }

  private boolean isDebitFlow(int hostFlow) {
    boolean isKnownDebitFlow =
        hostFlow == PosConstants.HostFlow.SOLUTION_SALE_AND_VISA_VALE_4
            || hostFlow == PosConstants.HostFlow.SOLUTION_SALE_AND_VISA_VALE_6
            || hostFlow == PosConstants.HostFlow.SOLUTION_SALE_AND_VISA_VALE_12
            || hostFlow == PosConstants.HostFlow.QUERY
            || hostFlow == PosConstants.HostFlow.TICKET;
    if (isKnownDebitFlow) {
      return true;
    }
    for (int[] range : PosConstants.HostFlow.DEBIT_RANGES_NEW) {
      if (hostFlow >= range[0] && hostFlow <= range[1]) {
        return true;
      }
    }
    return false;
  }

  private int parseIntOrZero(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private CanonicalTransaction.Operation.InstallmentPlan installmentPlan(
      PosTransaction transaction) {
    String financingType = "";
    int numberInstallments = 0;

    De61Parser.Subfield sub08 = subfield61(transaction, "08");
    if (sub08 != null && sub08.details() instanceof De61Parser.FinancingData details) {
      financingType = details.financingType().code();
      numberInstallments = details.installmentCount();
    }

    CanonicalTransaction.Operation.InstallmentPlan installmentPlan =
        new CanonicalTransaction.Operation.InstallmentPlan();
    installmentPlan.setFinancingType(financingType);
    installmentPlan.setNumberInstallments(numberInstallments);
    installmentPlan.setIsInstallment(numberInstallments > 1);
    return installmentPlan;
  }

  private CanonicalTransaction.Operation.Product product(PosTransaction transaction) {
    int matrixProduct = 0;
    De61Parser.Subfield sub14 = subfield61(transaction, "14");
    if (sub14 != null && sub14.details() instanceof De61Parser.MatrixProduct details) {
      matrixProduct = parseIntOrZero(details.productCode());
    }

    int secondaryProduct = 0;
    De61Parser.Subfield sub28 = subfield61(transaction, "28");
    if (sub28 != null && sub28.details() instanceof De61Parser.SecondaryProduct details) {
      secondaryProduct = parseIntOrZero(details.productCode());
    }

    CanonicalTransaction.Operation.Product.UniqueProduct uniqueProduct =
        new CanonicalTransaction.Operation.Product.UniqueProduct();
    uniqueProduct.setProduct(matrixProduct);
    uniqueProduct.setProductSub(secondaryProduct);

    CanonicalTransaction.Operation.Product.Settlement settlement =
        new CanonicalTransaction.Operation.Product.Settlement();
    settlement.setProduct(0);
    settlement.setProductSub(0);
    settlement.setCardType(0);

    CanonicalTransaction.Operation.Product product = new CanonicalTransaction.Operation.Product();
    product.setUniqueProduct(uniqueProduct);
    product.setSettlement(settlement);
    return product;
  }

  private CanonicalTransaction.Operation.Cdc cdc(int hostFlow, String processingCode) {
    CanonicalTransaction.Operation.Cdc cdc = new CanonicalTransaction.Operation.Cdc();
    cdc.setIsCDCConsultation(
        hostFlow == PosConstants.HostFlow.CDC_QUERY
            || hostFlow == PosConstants.HostFlow.BALANCE_QUERY);
    cdc.setIsCDCConsultationDebit(
        PosConstants.ProcessingCode.CDC_CONSULTING_DEBIT.equals(processingCode));
    return cdc;
  }

  private CanonicalTransaction.Operation.PrivateData privateData() {
    CanonicalTransaction.Operation.PrivateData privateData =
        new CanonicalTransaction.Operation.PrivateData();
    privateData.setIsPaymentCash(false);
    privateData.setIsPaymentCheck(false);
    return privateData;
  }

  private CanonicalTransaction.ConfirmacaoEnviada confirmacaoEnviada(PosTransaction transaction) {
    De62Parser.Data de62 = transaction.de062NetworkPrivateData1();
    if (de62 == null) {
      return null;
    }
    De62Parser.Subfield sub05 = de62.subfields().get("05");
    if (sub05 == null || !(sub05.details() instanceof De62Parser.EmvConfirmation details)) {
      return null;
    }
    if (details.documentNumber() == null || details.documentNumber().isBlank()) {
      return null;
    }

    CanonicalTransaction.ConfirmacaoEnviada confirmacaoEnviada =
        new CanonicalTransaction.ConfirmacaoEnviada();
    confirmacaoEnviada.setSubcampo("05");
    confirmacaoEnviada.setNsuOriginal(details.documentNumber());
    confirmacaoEnviada.setDataHoraTransacao(details.transactionDateTime());
    confirmacaoEnviada.setCodigoResposta(details.responseCode());
    confirmacaoEnviada.setDadosEmv(details.emvHex());
    confirmacaoEnviada.setEmvTags(details.emvTags());
    confirmacaoEnviada.setDadosAdicionais(details.additionalDataHex());
    return confirmacaoEnviada;
  }

  private List<Anotacao> anotacoes(PosTransaction transaction) {
    List<Anotacao> anotacoes = new ArrayList<>();
    De61Parser.Subfield sub08 = subfield61(transaction, "08");
    if (sub08 != null && sub08.details() instanceof De61Parser.FinancingData details) {
      String financingCode = details.financingType().code();
      boolean isZeroed =
          isBlankOrZero(financingCode, "00")
              && details.installmentCount() == 0
              && isBlankOrZero(details.preDatedDate(), "000000")
              && isBlankOrZero(details.numberOfDays(), "0000");
      if (isZeroed) {
        anotacoes.add(Anotacoes.sub6108Zeroed());
      }
    }
    return anotacoes;
  }

  private boolean isBlankOrZero(String value, String zeroValue) {
    return value == null || value.isEmpty() || zeroValue.equals(value);
  }

  private De61Parser.Subfield subfield61(PosTransaction transaction, String subfieldId) {
    De61Parser.Data de61 = transaction.de061PosPrivateData();
    return de61 == null ? null : de61.subfields().get(subfieldId);
  }
}
