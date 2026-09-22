# Runbook — como subir o ambiente

## Visão mental do projeto

Uma analogia útil para entender a aplicação:

> **Java fornece as peças, Maven organiza a oficina, Spring monta a máquina e Netty conecta a máquina à rede.**

- **Java 21** é a linguagem usada para construir as peças da aplicação.
- **Maven** organiza os módulos, baixa as dependências, compila e executa os testes.
- **Spring Boot** cria os serviços e conecta suas dependências.
- **Netty** recebe e envia as mensagens TCP dos terminais e das redes externas.
- **`pswitch-app`** reúne todos os módulos e inicia a aplicação.

O fluxo principal planejado pode ser entendido assim:

```text
Terminal POS
    ↓ TCP / Netty
pswitch-capture-pos
    ↓
pswitch-comum
    ↓
pswitch-nucleo
    ↓
pswitch-brand-visa
    ↓ TCP / Netty
Rede Visa
```

Na resposta, a transação percorre o caminho inverso até chegar novamente ao terminal.

## Pré-requisitos

- Java 21
- Maven
- Docker (pra subir o MongoDB via [`switch-docker`](../../../switch-docker))

## 0. Subir a infraestrutura (MongoDB)

```bash
cd D:\adq\switch-docker
docker compose up -d
```

## 1. Instalar os módulos no repositório local

Só na primeira vez, ou depois de mudar dependências entre módulos:

```bash
cd D:\adq\switch
mvn install -DskipTests
```

## 2. Rodar a aplicação

```bash
cd D:\adq\switch\pswitch-app
mvn spring-boot:run
```

O terminal fica ocupado (é servidor) — `Ctrl+C` pra derrubar.

Esperado ver no final:

```
Started SwitchApplication in X seconds
```

Se aparecer erro de conexão com `localhost:27017` (MongoDB), é porque o passo 0 (`docker compose up -d` no `switch-docker`) não foi feito — não derruba a aplicação, mas as funcionalidades que dependem do Mongo (registries, storage) não vão funcionar.

## 3. Testar com o simulador POS

Com a aplicação em execução, abra outro PowerShell na raiz do projeto e execute:

```powershell
cd D:\adq\switch
powershell -ExecutionPolicy Bypass -File .\tools\pos-simulator.ps1
```

O simulador contém uma mensagem POS binária de exemplo, converte o hexadecimal para bytes, adiciona automaticamente o prefixo binário de tamanho e envia para `127.0.0.1:9000`.

Para usar outro endereço ou porta:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\pos-simulator.ps1 `
  -ServerAddress 127.0.0.1 `
  -ServerPort 9000
```

Os dois primeiros bytes do frame são um inteiro binário sem sinal, em ordem de rede (big-endian). O transporte remove esse prefixo antes de entregar o payload ao POS e volta a adicioná-lo na resposta enviada pela mesma conexão.

## Padrão de código

O projeto segue o [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), aplicado automaticamente pelo formatador `google-java-format`. A ideia é remover qualquer discussão subjetiva de estilo (indentação, imports, quebra de linha) em code review — o formatador decide, não a pessoa.

### Formatação (Spotless)

O plugin `spotless-maven-plugin` está configurado no `pom.xml` raiz e é herdado por todos os módulos. Ele roda automaticamente na fase `validate`, então qualquer `mvn compile`, `mvn test` ou `mvn install` falha se algum arquivo estiver fora do padrão.

Se o build falhar por formatação, corrija com:

```bash
mvn spotless:apply
```

Isso reformata todos os módulos no lugar. Não precisa (e não deve) formatar manualmente.

### Análise estática (SpotBugs)

O `spotbugs-maven-plugin` também está declarado no `pom.xml` raiz, mas **não** está amarrado a nenhuma fase do build ainda — rodar manualmente quando quiser uma verificação mais profunda:

```bash
mvn compile spotbugs:check
```

Achados do tipo `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` ("pode expor representação interna") são deliberadamente ignorados via `spotbugs-exclude.xml` (na raiz do projeto): o modelo canônico (`CanonicalTransaction` e afins) é um DTO mutável passado só entre serviços internos do mesmo processo, e cópia defensiva em cada getter/setter só adicionaria boilerplate sem reduzir risco real. Outros tipos de achado (bug real de lógica, resource leak, etc.) devem ser investigados caso a caso.
