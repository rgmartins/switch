# switch

Switch de adquirente em Java (Spring Boot + Netty). Esqueleto multi-módulo Maven — só estrutura, sem lógica de negócio implementada ainda.

Referência de arquitetura: `C:\guzula\guzula-switch` (implementação em NestJS/TypeScript, funcional). Cada classe/arquivo aqui tem um comentário `Referência: ...` apontando pro arquivo equivalente lá.

## Módulos

```
pswitch-shared      domínio canônico (CanonicalTransaction), codec ISO 8583, CobolCodec
pswitch-transport   Netty: framing binário length-prefixed, servidor TCP inbound, pool outbound
pswitch-registry    registries: terminal / keyblock / bin
pswitch-external    integrações: antifraude, HSM, histórico, tarifas, pré-autorização
pswitch-comum       orquestra registries + HSM + antifraude antes do roteamento
pswitch-nucleo      motor de roteamento por bandeira (portas BrandHandler / ChannelResponder)
pswitch-brand-visa  conector Visa — implementa BrandHandler
pswitch-capture-pos interpreta o payload do terminal POS — implementa ChannelResponder
pswitch-liquidacao  liquidação/settlement via Kafka
pswitch-app         aplicação Spring Boot executável (main, application.yml, admin API)
```

Grafo de dependências (acíclico — por isso `nucleo` não depende de `brand-visa` nem de
`capture-pos` diretamente, e sim de interfaces `BrandHandler`/`ChannelResponder` que
esses módulos implementam; o Nest original resolve o mesmo problema com `forwardRef`,
Maven não tem equivalente):

```
shared, transport   (base)
registry, external  → shared
comum               → shared, registry, external
nucleo              → shared, comum
brand-visa          → shared, transport, nucleo
capture-pos         → shared, comum, nucleo
liquidacao          → shared
app                 → todos
```

## Stack

- Java 21, Maven (multi-módulo)
- Spring Boot 3.3 (DI, MongoDB, Redis, Kafka)
- Netty (camada de transporte TCP raw — tamanho binário de 2 bytes em ordem de rede)
- Codec ISO 8583 portado na mão (sem jPOS), pra manter paridade de comportamento com `iso-codec.ts`/`cobol-codec.ts`

## Build

```bash
mvn clean install
```

## Rodar

```bash
mvn -pl pswitch-app -am spring-boot:run
```

## Estado atual

Só esqueleto: assinaturas de classes/métodos espelhando os arquivos do `guzula-switch`,
cada um lançando `UnsupportedOperationException("TODO: portar ...")`. A implementação
vai sendo pedida módulo a módulo.
