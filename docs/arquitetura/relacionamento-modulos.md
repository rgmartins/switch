# Relacionamento entre os módulos

Este documento mostra a composição do switch, suas dependências de compilação e o fluxo esperado de uma autorização. O código atual ainda é um esqueleto; portanto, o segundo diagrama representa o fluxo arquitetural planejado.

## Dependências entre módulos

As setas significam **“depende de”**. O módulo `pswitch-app` é o ponto de composição: inicia o Spring Boot e reúne todos os demais módulos no mesmo processo.

```mermaid
flowchart TB
    app["pswitch-app<br/>composição e inicialização"]

    capture["pswitch-capture-pos<br/>canal de entrada POS"]
    visa["pswitch-brand-visa<br/>adaptador da bandeira Visa"]
    nucleo["pswitch-nucleo<br/>roteamento e portas"]
    comum["pswitch-comum<br/>pipeline compartilhado"]
    registry["pswitch-registry<br/>terminal, BIN e keyblock"]
    external["pswitch-external<br/>HSM, antifraude e serviços"]
    liquidacao["pswitch-liquidacao<br/>liquidação via Kafka"]
    transport["pswitch-transport<br/>TCP e framing"]
    shared["pswitch-shared<br/>domínio canônico e codecs"]

    app --> capture
    app --> visa
    app --> nucleo
    app --> comum
    app --> registry
    app --> external
    app --> liquidacao
    app --> transport
    app --> shared

    capture --> transport
    capture --> comum
    capture --> nucleo
    capture --> shared

    visa --> transport
    visa --> nucleo
    visa --> shared

    nucleo --> comum
    nucleo --> shared

    comum --> registry
    comum --> external
    comum --> shared

    registry --> shared
    external --> shared
    liquidacao --> shared

    classDef composition fill:#ede9fe,stroke:#6d28d9,color:#2e1065
    classDef domain fill:#dbeafe,stroke:#2563eb,color:#172554
    classDef adapter fill:#dcfce7,stroke:#16a34a,color:#052e16
    classDef infrastructure fill:#fef3c7,stroke:#d97706,color:#451a03

    class app composition
    class nucleo,comum,shared domain
    class capture,visa adapter
    class registry,external,liquidacao,transport infrastructure
```

### Papel das portas

O núcleo não conhece diretamente Visa nem POS. Ele publica dois contratos que invertem essas dependências:

```mermaid
flowchart LR
    nucleo["pswitch-nucleo"]
    brandPort{{"BrandHandler"}}
    channelPort{{"ChannelResponder"}}
    visa["VisaService"]
    pos["PosService"]

    nucleo -->|roteia autorização por bandeira| brandPort
    visa -.->|implementa| brandPort

    nucleo -->|devolve resposta ao canal de origem| channelPort
    pos -.->|implementa| channelPort
```

Essa inversão mantém o grafo Maven acíclico e permite incluir outras bandeiras ou canais sem criar uma dependência direta no núcleo.

## Fluxo planejado de uma autorização POS

```mermaid
sequenceDiagram
    autonumber
    actor Terminal as Terminal POS
    participant TCP as pswitch-transport
    participant POS as pswitch-capture-pos
    participant Comum as pswitch-comum
    participant Registry as pswitch-registry
    participant External as pswitch-external
    participant Nucleo as pswitch-nucleo
    participant Visa as pswitch-brand-visa
    participant Rede as Rede Visa

    Terminal->>TCP: mensagem ISO 8583 com framing BCD
    TCP->>POS: payload recebido
    POS->>POS: parse e mapeamento para CanonicalTransaction
    POS->>Comum: processa transação canônica
    Comum->>Registry: consulta terminal, BIN e keyblock
    Registry-->>Comum: dados de enriquecimento
    Comum->>External: HSM, antifraude e demais consultas
    External-->>Comum: resultados das validações
    Comum->>Nucleo: encaminha transação enriquecida
    Nucleo->>Visa: authorize via BrandHandler
    Visa->>Rede: mensagem da bandeira via TCP outbound
    Rede-->>Visa: resposta de autorização
    Visa->>Nucleo: resposta correlacionada
    Nucleo->>POS: sendResponse via ChannelResponder
    POS->>POS: converte e empacota a resposta
    POS->>TCP: payload de resposta
    TCP-->>Terminal: resposta ISO 8583 com framing BCD
```

## Leitura rápida

- `pswitch-shared` contém os contratos e codecs usados transversalmente.
- `pswitch-transport` cuida somente do transporte TCP e do enquadramento das mensagens.
- `pswitch-capture-pos` traduz o protocolo do terminal para o modelo canônico e vice-versa.
- `pswitch-comum` enriquece e valida a transação usando registries e integrações externas.
- `pswitch-nucleo` escolhe o adaptador de bandeira e retorna a resposta ao canal correto.
- `pswitch-brand-visa` traduz e transporta mensagens específicas da Visa.
- `pswitch-liquidacao` trata o fluxo assíncrono posterior de liquidação.

