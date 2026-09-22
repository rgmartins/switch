package com.guzula.pswitch.shared.domain;

import com.guzula.pswitch.shared.util.PrettyJson;
import java.util.List;
import java.util.Map;

/**
 * Modelo canônico de transação — contrato central lido/escrito por todos os módulos. Referência:
 * src/shared/domain/canonical-transaction.ts (guzula-switch).
 */
public class CanonicalTransaction {

  private String terminalId;
  private String nsu;
  private Operation operation;
  private Merchant merchant;
  private Equipment equipment;
  private Card card;
  private Security security;
  private Dcc dcc;
  private Tarifas tarifas;
  private Response response;
  private ConfirmacaoEnviada confirmacaoEnviada;
  private OriginalTransaction originalTransaction;
  private List<Anotacao> anotacoes;
  private Communication communication;
  private Antifraud antifraud;
  private List<RegistroLido> registros;
  private List<Interacao> interacoes;
  private Time time;

  public String getTerminalId() {
    return terminalId;
  }

  public void setTerminalId(String terminalId) {
    this.terminalId = terminalId;
  }

  public String getNsu() {
    return nsu;
  }

  public void setNsu(String nsu) {
    this.nsu = nsu;
  }

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  public Equipment getEquipment() {
    return equipment;
  }

  public void setEquipment(Equipment equipment) {
    this.equipment = equipment;
  }

  public Card getCard() {
    return card;
  }

  public void setCard(Card card) {
    this.card = card;
  }

  public Security getSecurity() {
    return security;
  }

  public void setSecurity(Security security) {
    this.security = security;
  }

  public Dcc getDcc() {
    return dcc;
  }

  public void setDcc(Dcc dcc) {
    this.dcc = dcc;
  }

  public Tarifas getTarifas() {
    return tarifas;
  }

  public void setTarifas(Tarifas tarifas) {
    this.tarifas = tarifas;
  }

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  public ConfirmacaoEnviada getConfirmacaoEnviada() {
    return confirmacaoEnviada;
  }

  public void setConfirmacaoEnviada(ConfirmacaoEnviada confirmacaoEnviada) {
    this.confirmacaoEnviada = confirmacaoEnviada;
  }

  public OriginalTransaction getOriginalTransaction() {
    return originalTransaction;
  }

  public void setOriginalTransaction(OriginalTransaction originalTransaction) {
    this.originalTransaction = originalTransaction;
  }

  public List<Anotacao> getAnotacoes() {
    return anotacoes;
  }

  public void setAnotacoes(List<Anotacao> anotacoes) {
    this.anotacoes = anotacoes;
  }

  public Communication getCommunication() {
    return communication;
  }

  public void setCommunication(Communication communication) {
    this.communication = communication;
  }

  public Antifraud getAntifraud() {
    return antifraud;
  }

  public void setAntifraud(Antifraud antifraud) {
    this.antifraud = antifraud;
  }

  public List<RegistroLido> getRegistros() {
    return registros;
  }

  public void setRegistros(List<RegistroLido> registros) {
    this.registros = registros;
  }

  public List<Interacao> getInteracoes() {
    return interacoes;
  }

  public void setInteracoes(List<Interacao> interacoes) {
    this.interacoes = interacoes;
  }

  public Time getTime() {
    return time;
  }

  public void setTime(Time time) {
    this.time = time;
  }

  @Override
  public String toString() {
    return PrettyJson.format(this);
  }

  public static class Operation {
    private String transactionModality; // indica se a transação é de débito ou crédito
    private String
        transactionType; // tipo de transação, ex: 100=crédito, 117=débito, 103=cancelamento,
    // 999=desfazimento...
    private String
        transactionSubtype; // se transactionType for 103 ou 999, indica o transactionType da
    // transação referenciada
    private InstallmentPlan installmentPlan;
    private Long serviceFee;
    private Long boardingFee;
    private Long entryValue;
    private long amount; // DE 4, em centavos
    private String currencyCode; // DE 49
    private String nsu; // extraido da captura
    private String keyUnique; // gerado pelo serviço key-unique
    private String dateTime;
    private String hostFlow; // extraido da captura
    private String digitalWallet;
    private Product product;
    private Boolean isPreAuth;
    private Boolean isUndo;
    private Boolean isRecurringPayment;
    private Cdc cdc;
    private PrivateData privateData; // TS: "private"

    public String getTransactionModality() {
      return transactionModality;
    }

    public void setTransactionModality(String transactionModality) {
      this.transactionModality = transactionModality;
    }

    public String getTransactionType() {
      return transactionType;
    }

    public void setTransactionType(String transactionType) {
      this.transactionType = transactionType;
    }

    public String getTransactionSubtype() {
      return transactionSubtype;
    }

    public void setTransactionSubtype(String transactionSubtype) {
      this.transactionSubtype = transactionSubtype;
    }

    public InstallmentPlan getInstallmentPlan() {
      return installmentPlan;
    }

    public void setInstallmentPlan(InstallmentPlan installmentPlan) {
      this.installmentPlan = installmentPlan;
    }

    public Long getServiceFee() {
      return serviceFee;
    }

    public void setServiceFee(Long serviceFee) {
      this.serviceFee = serviceFee;
    }

    public Long getBoardingFee() {
      return boardingFee;
    }

    public void setBoardingFee(Long boardingFee) {
      this.boardingFee = boardingFee;
    }

    public Long getEntryValue() {
      return entryValue;
    }

    public void setEntryValue(Long entryValue) {
      this.entryValue = entryValue;
    }

    public long getAmount() {
      return amount;
    }

    public void setAmount(long amount) {
      this.amount = amount;
    }

    public String getCurrencyCode() {
      return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
      this.currencyCode = currencyCode;
    }

    public String getNsu() {
      return nsu;
    }

    public void setNsu(String nsu) {
      this.nsu = nsu;
    }

    public String getKeyUnique() {
      return keyUnique;
    }

    public void setKeyUnique(String keyUnique) {
      this.keyUnique = keyUnique;
    }

    public String getDateTime() {
      return dateTime;
    }

    public void setDateTime(String dateTime) {
      this.dateTime = dateTime;
    }

    public String getHostFlow() {
      return hostFlow;
    }

    public void setHostFlow(String hostFlow) {
      this.hostFlow = hostFlow;
    }

    public String getDigitalWallet() {
      return digitalWallet;
    }

    public void setDigitalWallet(String digitalWallet) {
      this.digitalWallet = digitalWallet;
    }

    public Product getProduct() {
      return product;
    }

    public void setProduct(Product product) {
      this.product = product;
    }

    public Boolean getIsPreAuth() {
      return isPreAuth;
    }

    public void setIsPreAuth(Boolean isPreAuth) {
      this.isPreAuth = isPreAuth;
    }

    public Boolean getIsUndo() {
      return isUndo;
    }

    public void setIsUndo(Boolean isUndo) {
      this.isUndo = isUndo;
    }

    public Boolean getIsRecurringPayment() {
      return isRecurringPayment;
    }

    public void setIsRecurringPayment(Boolean isRecurringPayment) {
      this.isRecurringPayment = isRecurringPayment;
    }

    public Cdc getCdc() {
      return cdc;
    }

    public void setCdc(Cdc cdc) {
      this.cdc = cdc;
    }

    public PrivateData getPrivateData() {
      return privateData;
    }

    public void setPrivateData(PrivateData privateData) {
      this.privateData = privateData;
    }

    public static class InstallmentPlan {
      private String financingType;
      private Integer numberInstallments;
      private Boolean isInstallment;

      public String getFinancingType() {
        return financingType;
      }

      public void setFinancingType(String financingType) {
        this.financingType = financingType;
      }

      public Integer getNumberInstallments() {
        return numberInstallments;
      }

      public void setNumberInstallments(Integer numberInstallments) {
        this.numberInstallments = numberInstallments;
      }

      public Boolean getIsInstallment() {
        return isInstallment;
      }

      public void setIsInstallment(Boolean isInstallment) {
        this.isInstallment = isInstallment;
      }
    }

    public static class Product {
      private UniqueProduct uniqueProduct;
      private Settlement settlement;

      public UniqueProduct getUniqueProduct() {
        return uniqueProduct;
      }

      public void setUniqueProduct(UniqueProduct uniqueProduct) {
        this.uniqueProduct = uniqueProduct;
      }

      public Settlement getSettlement() {
        return settlement;
      }

      public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
      }

      /** Extraido da captura POS; para TEF é feito uma conversão para o produto único. */
      public static class UniqueProduct {
        private Integer product;
        private Integer productSub;

        public Integer getProduct() {
          return product;
        }

        public void setProduct(Integer product) {
          this.product = product;
        }

        public Integer getProductSub() {
          return productSub;
        }

        public void setProductSub(Integer productSub) {
          this.productSub = productSub;
        }
      }

      /** Convertido de produto único para o produto da bandeira. */
      public static class Settlement {
        private Integer product;
        private Integer productSub;
        private Integer cardType;

        public Integer getProduct() {
          return product;
        }

        public void setProduct(Integer product) {
          this.product = product;
        }

        public Integer getProductSub() {
          return productSub;
        }

        public void setProductSub(Integer productSub) {
          this.productSub = productSub;
        }

        public Integer getCardType() {
          return cardType;
        }

        public void setCardType(Integer cardType) {
          this.cardType = cardType;
        }
      }
    }

    public static class Cdc {
      private Boolean
          isCDCConsultation; // definido pelo fluxo do host (DE24 V32 = '0006' ou '0013')
      private Boolean isCDCConsultationDebit; // definido pelo código de processamento = '380010'

      public Boolean getIsCDCConsultation() {
        return isCDCConsultation;
      }

      public void setIsCDCConsultation(Boolean isCDCConsultation) {
        this.isCDCConsultation = isCDCConsultation;
      }

      public Boolean getIsCDCConsultationDebit() {
        return isCDCConsultationDebit;
      }

      public void setIsCDCConsultationDebit(Boolean isCDCConsultationDebit) {
        this.isCDCConsultationDebit = isCDCConsultationDebit;
      }
    }

    /** Definido através do produto. TS: "private". */
    public static class PrivateData {
      private Boolean isPaymentCash;
      private Boolean isPaymentCheck;

      public Boolean getIsPaymentCash() {
        return isPaymentCash;
      }

      public void setIsPaymentCash(Boolean isPaymentCash) {
        this.isPaymentCash = isPaymentCash;
      }

      public Boolean getIsPaymentCheck() {
        return isPaymentCheck;
      }

      public void setIsPaymentCheck(Boolean isPaymentCheck) {
        this.isPaymentCheck = isPaymentCheck;
      }
    }
  }

  public static class Merchant {
    private String name; // extraido do arquivo de terminal
    private String merchant;
    private String store;
    private String merchantPhysical; // extraido da captura, só TEF; POS não tem
    private String storePhysical; // extraido da captura, só TEF; POS não tem
    private String merchantHeadquarters; // código do estabelecimento matriz
    private String storeHeadquarters; // loja do estabelecimento matriz
    private Address address;
    private String merchantCategoryCode;
    private String person; // TypePersona: pessoa física ou jurídica
    private String cnpjOrCpf; // se pessoa jurídica, o CNPJ, senão o CPF

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getMerchant() {
      return merchant;
    }

    public void setMerchant(String merchant) {
      this.merchant = merchant;
    }

    public String getStore() {
      return store;
    }

    public void setStore(String store) {
      this.store = store;
    }

    public String getMerchantPhysical() {
      return merchantPhysical;
    }

    public void setMerchantPhysical(String merchantPhysical) {
      this.merchantPhysical = merchantPhysical;
    }

    public String getStorePhysical() {
      return storePhysical;
    }

    public void setStorePhysical(String storePhysical) {
      this.storePhysical = storePhysical;
    }

    public String getMerchantHeadquarters() {
      return merchantHeadquarters;
    }

    public void setMerchantHeadquarters(String merchantHeadquarters) {
      this.merchantHeadquarters = merchantHeadquarters;
    }

    public String getStoreHeadquarters() {
      return storeHeadquarters;
    }

    public void setStoreHeadquarters(String storeHeadquarters) {
      this.storeHeadquarters = storeHeadquarters;
    }

    public Address getAddress() {
      return address;
    }

    public void setAddress(Address address) {
      this.address = address;
    }

    public String getMerchantCategoryCode() {
      return merchantCategoryCode;
    }

    public void setMerchantCategoryCode(String merchantCategoryCode) {
      this.merchantCategoryCode = merchantCategoryCode;
    }

    public String getPerson() {
      return person;
    }

    public void setPerson(String person) {
      this.person = person;
    }

    public String getCnpjOrCpf() {
      return cnpjOrCpf;
    }

    public void setCnpjOrCpf(String cnpjOrCpf) {
      this.cnpjOrCpf = cnpjOrCpf;
    }

    /** Todos os campos extraidos do arquivo de terminal. */
    public static class Address {
      private String street;
      private String number;
      private String complement;
      private String neighborhood;
      private String city;
      private String zipCode;
      private String uf;
      private String country;

      public String getStreet() {
        return street;
      }

      public void setStreet(String street) {
        this.street = street;
      }

      public String getNumber() {
        return number;
      }

      public void setNumber(String number) {
        this.number = number;
      }

      public String getComplement() {
        return complement;
      }

      public void setComplement(String complement) {
        this.complement = complement;
      }

      public String getNeighborhood() {
        return neighborhood;
      }

      public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
      }

      public String getCity() {
        return city;
      }

      public void setCity(String city) {
        this.city = city;
      }

      public String getZipCode() {
        return zipCode;
      }

      public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
      }

      public String getUf() {
        return uf;
      }

      public void setUf(String uf) {
        this.uf = uf;
      }

      public String getCountry() {
        return country;
      }

      public void setCountry(String country) {
        this.country = country;
      }
    }
  }

  public static class Equipment {
    private String mobilePaymentType; // TODO: carregar a partir do cadastro de terminais
    private String type; // TypeEquipment
    private String terminalId; // DE 41
    private String terminalOrigin; // usado pelo TEF
    private String idSoftware; // extraido da captura
    private String numberSeriePos; // extraido da captura, só POS; TEF não tem
    private String numberSeriePinpad; // extraido da captura
    private String dateTimeLocal;
    private Boolean doesPreAuth;
    private PinPad pinPad;
    private Boolean doesDcc; // vem do terminal, indica se o equipamento aceita transação DCC
    private Boolean terminalBlocked;

    public String getMobilePaymentType() {
      return mobilePaymentType;
    }

    public void setMobilePaymentType(String mobilePaymentType) {
      this.mobilePaymentType = mobilePaymentType;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getTerminalId() {
      return terminalId;
    }

    public void setTerminalId(String terminalId) {
      this.terminalId = terminalId;
    }

    public String getTerminalOrigin() {
      return terminalOrigin;
    }

    public void setTerminalOrigin(String terminalOrigin) {
      this.terminalOrigin = terminalOrigin;
    }

    public String getIdSoftware() {
      return idSoftware;
    }

    public void setIdSoftware(String idSoftware) {
      this.idSoftware = idSoftware;
    }

    public String getNumberSeriePos() {
      return numberSeriePos;
    }

    public void setNumberSeriePos(String numberSeriePos) {
      this.numberSeriePos = numberSeriePos;
    }

    public String getNumberSeriePinpad() {
      return numberSeriePinpad;
    }

    public void setNumberSeriePinpad(String numberSeriePinpad) {
      this.numberSeriePinpad = numberSeriePinpad;
    }

    public String getDateTimeLocal() {
      return dateTimeLocal;
    }

    public void setDateTimeLocal(String dateTimeLocal) {
      this.dateTimeLocal = dateTimeLocal;
    }

    public Boolean getDoesPreAuth() {
      return doesPreAuth;
    }

    public void setDoesPreAuth(Boolean doesPreAuth) {
      this.doesPreAuth = doesPreAuth;
    }

    public PinPad getPinPad() {
      return pinPad;
    }

    public void setPinPad(PinPad pinPad) {
      this.pinPad = pinPad;
    }

    public Boolean getDoesDcc() {
      return doesDcc;
    }

    public void setDoesDcc(Boolean doesDcc) {
      this.doesDcc = doesDcc;
    }

    public Boolean getTerminalBlocked() {
      return terminalBlocked;
    }

    public void setTerminalBlocked(Boolean terminalBlocked) {
      this.terminalBlocked = terminalBlocked;
    }

    public static class PinPad {
      private Boolean hasPinpad; // define se o equipamento tem pinpad
      private Boolean hasPinpadChip; // define se o equipamento tem pinpad com chip

      public Boolean getHasPinpad() {
        return hasPinpad;
      }

      public void setHasPinpad(Boolean hasPinpad) {
        this.hasPinpad = hasPinpad;
      }

      public Boolean getHasPinpadChip() {
        return hasPinpadChip;
      }

      public void setHasPinpadChip(Boolean hasPinpadChip) {
        this.hasPinpadChip = hasPinpadChip;
      }
    }
  }

  public static class Card {
    private String inputMethod; // InputMethod, extraido da captura
    private Boolean isContactless;
    private Boolean isTapOnPhone; // vem do campo 22 do TEF 4.1
    private String cardNumber;
    private String expirationDate;
    private Cvv cvv;
    private CardBrand cardBrand;
    private Boolean isForeign; // vem do cadastro de bins
    private Boolean isCorporate; // vem do cadastro de bins
    private Boolean isDebit; // vem do cadastro de bins
    private Boolean isCredit; // vem do cadastro de bins
    private Boolean isDebitMaster; // vem do cadastro de bins
    private String product; // contém uma espécie de produto do bin, ex: C = Carne (APIBIR-TIPO)
    private String country;
    private String track;
    private String serviceCode; // extraido da trilha1 ou trilha2
    private String cardSequenceNumber; // DE 23
    private String cardholderName; // EMV tag 5F20
    private Emv emv;

    public String getInputMethod() {
      return inputMethod;
    }

    public void setInputMethod(String inputMethod) {
      this.inputMethod = inputMethod;
    }

    public Boolean getIsContactless() {
      return isContactless;
    }

    public void setIsContactless(Boolean isContactless) {
      this.isContactless = isContactless;
    }

    public Boolean getIsTapOnPhone() {
      return isTapOnPhone;
    }

    public void setIsTapOnPhone(Boolean isTapOnPhone) {
      this.isTapOnPhone = isTapOnPhone;
    }

    public String getCardNumber() {
      return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
      this.cardNumber = cardNumber;
    }

    public String getExpirationDate() {
      return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
      this.expirationDate = expirationDate;
    }

    public Cvv getCvv() {
      return cvv;
    }

    public void setCvv(Cvv cvv) {
      this.cvv = cvv;
    }

    public CardBrand getCardBrand() {
      return cardBrand;
    }

    public void setCardBrand(CardBrand cardBrand) {
      this.cardBrand = cardBrand;
    }

    public Boolean getIsForeign() {
      return isForeign;
    }

    public void setIsForeign(Boolean isForeign) {
      this.isForeign = isForeign;
    }

    public Boolean getIsCorporate() {
      return isCorporate;
    }

    public void setIsCorporate(Boolean isCorporate) {
      this.isCorporate = isCorporate;
    }

    public Boolean getIsDebit() {
      return isDebit;
    }

    public void setIsDebit(Boolean isDebit) {
      this.isDebit = isDebit;
    }

    public Boolean getIsCredit() {
      return isCredit;
    }

    public void setIsCredit(Boolean isCredit) {
      this.isCredit = isCredit;
    }

    public Boolean getIsDebitMaster() {
      return isDebitMaster;
    }

    public void setIsDebitMaster(Boolean isDebitMaster) {
      this.isDebitMaster = isDebitMaster;
    }

    public String getProduct() {
      return product;
    }

    public void setProduct(String product) {
      this.product = product;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }

    public String getTrack() {
      return track;
    }

    public void setTrack(String track) {
      this.track = track;
    }

    public String getServiceCode() {
      return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
      this.serviceCode = serviceCode;
    }

    public String getCardSequenceNumber() {
      return cardSequenceNumber;
    }

    public void setCardSequenceNumber(String cardSequenceNumber) {
      this.cardSequenceNumber = cardSequenceNumber;
    }

    public String getCardholderName() {
      return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
      this.cardholderName = cardholderName;
    }

    public Emv getEmv() {
      return emv;
    }

    public void setEmv(Emv emv) {
      this.emv = emv;
    }

    /** Extraido da captura. */
    public static class Cvv {
      private Integer cvvIndicator;
      private String cvvValue;

      public Integer getCvvIndicator() {
        return cvvIndicator;
      }

      public void setCvvIndicator(Integer cvvIndicator) {
        this.cvvIndicator = cvvIndicator;
      }

      public String getCvvValue() {
        return cvvValue;
      }

      public void setCvvValue(String cvvValue) {
        this.cvvValue = cvvValue;
      }
    }

    /**
     * Todos os campos vêm do cadastro de bandeiras, exceto authorization, usado pelo NucleoService
     * para rotear.
     */
    public static class CardBrand {
      private String authorization; // bandeira da autorização
      private String register; // bandeira para acessar o cadastro
      private String settlement; // bandeira da liquidação
      private Boolean doesDcc; // indica se a bandeira aceita DCC
      private Boolean doesPreAuth; // indica se a bandeira aceita pré-autorização
      private Boolean active; // indica se a bandeira está ativa no switch
      private Long transactionMinValue; // valor mínimo de transação
      private String settlementModel; // modelo de liquidação com o EC: FULL, VAN ou HIBRIDO

      public String getAuthorization() {
        return authorization;
      }

      public void setAuthorization(String authorization) {
        this.authorization = authorization;
      }

      public String getRegister() {
        return register;
      }

      public void setRegister(String register) {
        this.register = register;
      }

      public String getSettlement() {
        return settlement;
      }

      public void setSettlement(String settlement) {
        this.settlement = settlement;
      }

      public Boolean getDoesDcc() {
        return doesDcc;
      }

      public void setDoesDcc(Boolean doesDcc) {
        this.doesDcc = doesDcc;
      }

      public Boolean getDoesPreAuth() {
        return doesPreAuth;
      }

      public void setDoesPreAuth(Boolean doesPreAuth) {
        this.doesPreAuth = doesPreAuth;
      }

      public Boolean getActive() {
        return active;
      }

      public void setActive(Boolean active) {
        this.active = active;
      }

      public Long getTransactionMinValue() {
        return transactionMinValue;
      }

      public void setTransactionMinValue(Long transactionMinValue) {
        this.transactionMinValue = transactionMinValue;
      }

      public String getSettlementModel() {
        return settlementModel;
      }

      public void setSettlementModel(String settlementModel) {
        this.settlementModel = settlementModel;
      }
    }
  }

  /** Extraido da captura. Reaproveitado por card.emv e response.emv. */
  public static class Emv {
    private List<EmvTag> tags;

    public List<EmvTag> getTags() {
      return tags;
    }

    public void setTags(List<EmvTag> tags) {
      this.tags = tags;
    }
  }

  public static class EmvTag {
    private String tag;
    private Integer size;
    private String value;

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public Integer getSize() {
      return size;
    }

    public void setSize(Integer size) {
      this.size = size;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class Security {
    private Ksn ksn;
    private String encryptedCardData;
    private String pinBlock;

    public Ksn getKsn() {
      return ksn;
    }

    public void setKsn(Ksn ksn) {
      this.ksn = ksn;
    }

    public String getEncryptedCardData() {
      return encryptedCardData;
    }

    public void setEncryptedCardData(String encryptedCardData) {
      this.encryptedCardData = encryptedCardData;
    }

    public String getPinBlock() {
      return pinBlock;
    }

    public void setPinBlock(String pinBlock) {
      this.pinBlock = pinBlock;
    }

    public static class Ksn {
      private String bdkIndicator;
      private String pinPadIndicator;
      private String transactionCounter;

      public String getBdkIndicator() {
        return bdkIndicator;
      }

      public void setBdkIndicator(String bdkIndicator) {
        this.bdkIndicator = bdkIndicator;
      }

      public String getPinPadIndicator() {
        return pinPadIndicator;
      }

      public void setPinPadIndicator(String pinPadIndicator) {
        this.pinPadIndicator = pinPadIndicator;
      }

      public String getTransactionCounter() {
        return transactionCounter;
      }

      public void setTransactionCounter(String transactionCounter) {
        this.transactionCounter = transactionCounter;
      }
    }
  }

  public static class Dcc {
    private String responseCode;
    private Long billingAmount;
    private String billingCurrencyCode;
    private String conversionRate;

    public String getResponseCode() {
      return responseCode;
    }

    public void setResponseCode(String responseCode) {
      this.responseCode = responseCode;
    }

    public Long getBillingAmount() {
      return billingAmount;
    }

    public void setBillingAmount(Long billingAmount) {
      this.billingAmount = billingAmount;
    }

    public String getBillingCurrencyCode() {
      return billingCurrencyCode;
    }

    public void setBillingCurrencyCode(String billingCurrencyCode) {
      this.billingCurrencyCode = billingCurrencyCode;
    }

    public String getConversionRate() {
      return conversionRate;
    }

    public void setConversionRate(String conversionRate) {
      this.conversionRate = conversionRate;
    }
  }

  public static class Tarifas {
    private String responseCode;
    private List<Occurrence> occurrences;

    public String getResponseCode() {
      return responseCode;
    }

    public void setResponseCode(String responseCode) {
      this.responseCode = responseCode;
    }

    public List<Occurrence> getOccurrences() {
      return occurrences;
    }

    public void setOccurrences(List<Occurrence> occurrences) {
      this.occurrences = occurrences;
    }

    public static class Occurrence {
      private Integer installmentCount;
      private Long installmentValue;
      private Long totalTransaction;
      private Double factor1;
      private Double factor2;
      private Integer settleDays;
      private Long firstInstallmentValue;
      private Long otherInstallmentsValue;
      private Double discountRate;
      private Long originalAmount;
      private String periodicity;
      private Double monthlyRate;
      private Double eftCostRate;

      public Integer getInstallmentCount() {
        return installmentCount;
      }

      public void setInstallmentCount(Integer installmentCount) {
        this.installmentCount = installmentCount;
      }

      public Long getInstallmentValue() {
        return installmentValue;
      }

      public void setInstallmentValue(Long installmentValue) {
        this.installmentValue = installmentValue;
      }

      public Long getTotalTransaction() {
        return totalTransaction;
      }

      public void setTotalTransaction(Long totalTransaction) {
        this.totalTransaction = totalTransaction;
      }

      public Double getFactor1() {
        return factor1;
      }

      public void setFactor1(Double factor1) {
        this.factor1 = factor1;
      }

      public Double getFactor2() {
        return factor2;
      }

      public void setFactor2(Double factor2) {
        this.factor2 = factor2;
      }

      public Integer getSettleDays() {
        return settleDays;
      }

      public void setSettleDays(Integer settleDays) {
        this.settleDays = settleDays;
      }

      public Long getFirstInstallmentValue() {
        return firstInstallmentValue;
      }

      public void setFirstInstallmentValue(Long firstInstallmentValue) {
        this.firstInstallmentValue = firstInstallmentValue;
      }

      public Long getOtherInstallmentsValue() {
        return otherInstallmentsValue;
      }

      public void setOtherInstallmentsValue(Long otherInstallmentsValue) {
        this.otherInstallmentsValue = otherInstallmentsValue;
      }

      public Double getDiscountRate() {
        return discountRate;
      }

      public void setDiscountRate(Double discountRate) {
        this.discountRate = discountRate;
      }

      public Long getOriginalAmount() {
        return originalAmount;
      }

      public void setOriginalAmount(Long originalAmount) {
        this.originalAmount = originalAmount;
      }

      public String getPeriodicity() {
        return periodicity;
      }

      public void setPeriodicity(String periodicity) {
        this.periodicity = periodicity;
      }

      public Double getMonthlyRate() {
        return monthlyRate;
      }

      public void setMonthlyRate(Double monthlyRate) {
        this.monthlyRate = monthlyRate;
      }

      public Double getEftCostRate() {
        return eftCostRate;
      }

      public void setEftCostRate(Double eftCostRate) {
        this.eftCostRate = eftCostRate;
      }
    }
  }

  public static class Response {
    private String whoResponded;
    private Obs obs;
    private String responseCode;
    private String issuerResponseCode;
    private String message;
    private String messageSource;
    private String authorizationCode;
    private Emv emv;
    private String paymentAccountReference;
    private Mastercard mastercard;
    private Error error;

    public String getWhoResponded() {
      return whoResponded;
    }

    public void setWhoResponded(String whoResponded) {
      this.whoResponded = whoResponded;
    }

    public Obs getObs() {
      return obs;
    }

    public void setObs(Obs obs) {
      this.obs = obs;
    }

    public String getResponseCode() {
      return responseCode;
    }

    public void setResponseCode(String responseCode) {
      this.responseCode = responseCode;
    }

    public String getIssuerResponseCode() {
      return issuerResponseCode;
    }

    public void setIssuerResponseCode(String issuerResponseCode) {
      this.issuerResponseCode = issuerResponseCode;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getMessageSource() {
      return messageSource;
    }

    public void setMessageSource(String messageSource) {
      this.messageSource = messageSource;
    }

    public String getAuthorizationCode() {
      return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
      this.authorizationCode = authorizationCode;
    }

    public Emv getEmv() {
      return emv;
    }

    public void setEmv(Emv emv) {
      this.emv = emv;
    }

    public String getPaymentAccountReference() {
      return paymentAccountReference;
    }

    public void setPaymentAccountReference(String paymentAccountReference) {
      this.paymentAccountReference = paymentAccountReference;
    }

    public Mastercard getMastercard() {
      return mastercard;
    }

    public void setMastercard(Mastercard mastercard) {
      this.mastercard = mastercard;
    }

    public Error getError() {
      return error;
    }

    public void setError(Error error) {
      this.error = error;
    }

    public static class Obs {
      private Integer code;
      private String description;

      public Integer getCode() {
        return code;
      }

      public void setCode(Integer code) {
        this.code = code;
      }

      public String getDescription() {
        return description;
      }

      public void setDescription(String description) {
        this.description = description;
      }
    }

    public static class Mastercard {
      private Network network;
      private String settlementDate;

      public Network getNetwork() {
        return network;
      }

      public void setNetwork(Network network) {
        this.network = network;
      }

      public String getSettlementDate() {
        return settlementDate;
      }

      public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate;
      }

      public static class Network {
        private String financialNetworkCode;
        private String banknetReferenceNumber;

        public String getFinancialNetworkCode() {
          return financialNetworkCode;
        }

        public void setFinancialNetworkCode(String financialNetworkCode) {
          this.financialNetworkCode = financialNetworkCode;
        }

        public String getBanknetReferenceNumber() {
          return banknetReferenceNumber;
        }

        public void setBanknetReferenceNumber(String banknetReferenceNumber) {
          this.banknetReferenceNumber = banknetReferenceNumber;
        }
      }
    }

    public static class Error {
      private String message;
      private String stack;

      public String getMessage() {
        return message;
      }

      public void setMessage(String message) {
        this.message = message;
      }

      public String getStack() {
        return stack;
      }

      public void setStack(String stack) {
        this.stack = stack;
      }
    }
  }

  public static class ConfirmacaoEnviada {
    private String subcampo;
    private String nsuOriginal;
    private String dataHoraTransacao;
    private String codigoResposta;
    private String dadosEmv;
    private Map<String, String> emvTags;
    private String dadosAdicionais;

    public String getSubcampo() {
      return subcampo;
    }

    public void setSubcampo(String subcampo) {
      this.subcampo = subcampo;
    }

    public String getNsuOriginal() {
      return nsuOriginal;
    }

    public void setNsuOriginal(String nsuOriginal) {
      this.nsuOriginal = nsuOriginal;
    }

    public String getDataHoraTransacao() {
      return dataHoraTransacao;
    }

    public void setDataHoraTransacao(String dataHoraTransacao) {
      this.dataHoraTransacao = dataHoraTransacao;
    }

    public String getCodigoResposta() {
      return codigoResposta;
    }

    public void setCodigoResposta(String codigoResposta) {
      this.codigoResposta = codigoResposta;
    }

    public String getDadosEmv() {
      return dadosEmv;
    }

    public void setDadosEmv(String dadosEmv) {
      this.dadosEmv = dadosEmv;
    }

    public Map<String, String> getEmvTags() {
      return emvTags;
    }

    public void setEmvTags(Map<String, String> emvTags) {
      this.emvTags = emvTags;
    }

    public String getDadosAdicionais() {
      return dadosAdicionais;
    }

    public void setDadosAdicionais(String dadosAdicionais) {
      this.dadosAdicionais = dadosAdicionais;
    }
  }

  public static class OriginalTransaction {
    private String id;
    private String nsu;
    private String terminalId;
    private String date;
    private String authorizationCode;
    private Long amount;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getNsu() {
      return nsu;
    }

    public void setNsu(String nsu) {
      this.nsu = nsu;
    }

    public String getTerminalId() {
      return terminalId;
    }

    public void setTerminalId(String terminalId) {
      this.terminalId = terminalId;
    }

    public String getDate() {
      return date;
    }

    public void setDate(String date) {
      this.date = date;
    }

    public String getAuthorizationCode() {
      return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
      this.authorizationCode = authorizationCode;
    }

    public Long getAmount() {
      return amount;
    }

    public void setAmount(Long amount) {
      this.amount = amount;
    }
  }

  public static class Communication {
    private String socketId;
    private String channel; // TypeChannel: POS | TEF

    public String getSocketId() {
      return socketId;
    }

    public void setSocketId(String socketId) {
      this.socketId = socketId;
    }

    public String getChannel() {
      return channel;
    }

    public void setChannel(String channel) {
      this.channel = channel;
    }
  }

  public static class Antifraud {
    private String acao;
    private String scoreRegra;
    private String regraLynx;
    private String scoreNeural;
    private String indBehaviour;
    private String regraAutoriz;
    private String codResp;

    public String getAcao() {
      return acao;
    }

    public void setAcao(String acao) {
      this.acao = acao;
    }

    public String getScoreRegra() {
      return scoreRegra;
    }

    public void setScoreRegra(String scoreRegra) {
      this.scoreRegra = scoreRegra;
    }

    public String getRegraLynx() {
      return regraLynx;
    }

    public void setRegraLynx(String regraLynx) {
      this.regraLynx = regraLynx;
    }

    public String getScoreNeural() {
      return scoreNeural;
    }

    public void setScoreNeural(String scoreNeural) {
      this.scoreNeural = scoreNeural;
    }

    public String getIndBehaviour() {
      return indBehaviour;
    }

    public void setIndBehaviour(String indBehaviour) {
      this.indBehaviour = indBehaviour;
    }

    public String getRegraAutoriz() {
      return regraAutoriz;
    }

    public void setRegraAutoriz(String regraAutoriz) {
      this.regraAutoriz = regraAutoriz;
    }

    public String getCodResp() {
      return codResp;
    }

    public void setCodResp(String codResp) {
      this.codResp = codResp;
    }
  }

  public static class Time {
    private Fase ida;
    private Fase volta;

    public Fase getIda() {
      return ida;
    }

    public void setIda(Fase ida) {
      this.ida = ida;
    }

    public Fase getVolta() {
      return volta;
    }

    public void setVolta(Fase volta) {
      this.volta = volta;
    }

    public static class Fase {
      private String start;
      private String end;
      private Long durationMs;

      public String getStart() {
        return start;
      }

      public void setStart(String start) {
        this.start = start;
      }

      public String getEnd() {
        return end;
      }

      public void setEnd(String end) {
        this.end = end;
      }

      public Long getDurationMs() {
        return durationMs;
      }

      public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
      }
    }
  }
}
