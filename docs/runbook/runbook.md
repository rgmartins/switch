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
- Node.js 22 ou superior (para executar os simuladores localmente)
- Docker (pra subir o MongoDB via [`switch-docker`](../../../switch-docker))

## 0. Subir a infraestrutura (MongoDB, Redis)

```bash
cd D:\adq\switch-docker
docker compose up -d
```

O Redis é usado pela correlação de resposta assíncrona (Visa, HSM) e pelas filas entre o
`pswitch-comunicacao` e o switch — ver docs/arquitetura/topologia-implantacao.md.

## 1. Instalar os módulos no repositório local

Só na primeira vez, ou depois de mudar dependências entre módulos:

```bash
cd D:\adq\switch
mvn install -DskipTests
```

## 2. Rodar o processo de comunicação (`pswitch-comunicacao`)

Processo separado do switch — segura as três conexões reais (POS, Visa, HSM). O `pswitch-app` não
abre socket nenhum; só fala com filas do Redis. Precisa estar de pé **antes** do switch, senão os
pedidos ficam esperando na fila até estourar timeout, e nenhum terminal POS consegue conectar
(quem ouve a porta 9000 agora é este processo, não o switch).

```bash
cd D:\adq\switch\pswitch-comunicacao
mvn spring-boot:run
```

Esperado ver no final:

```
Cliente TCP de HSM configurado para 127.0.0.1:6002
Cliente TCP de VISA configurado para 127.0.0.1:5000
Servidor TCP de POS ouvindo em 0.0.0.0:9000
Started ComunicacaoApplication in X seconds
```

Pra rodar o `.jar` já empacotado (sem `mvn spring-boot:run`), o executável tem um classificador
próprio — o `.jar` sem classificador é só a biblioteca que `pswitch-external` usa em tempo de
compilação, não roda sozinho:

```bash
cd D:\adq\switch\pswitch-comunicacao
mvn -DskipTests package
java -jar target\pswitch-comunicacao-0.1.0-SNAPSHOT-exec.jar
```

## 3. Rodar o switch (`pswitch-app`)

```bash
cd D:\adq\switch\pswitch-app
mvn spring-boot:run
```

O terminal fica ocupado (é servidor) — `Ctrl+C` pra derrubar.

Esperado ver no final:

```
Started SwitchApplication in X seconds
```

Repare que **não aparece nenhuma linha de "Cliente TCP"/"Servidor TCP"** — esse processo não abre
socket nenhum; só fala com as filas do Redis que o `pswitch-comunicacao` (passo 2) atende.

Se aparecer erro de conexão com `localhost:27017` (MongoDB), é porque o passo 0 (`docker compose up -d` no `switch-docker`) não foi feito — não derruba a aplicação, mas as funcionalidades que dependem do Mongo (registries, storage) não vão funcionar.

## 4. Subir o simulador HSM

Em outro PowerShell, execute:

```powershell
cd D:\adq\switch-simuladores
npm run start:dev
```

Por padrão, o simulador HSM fica disponível em `0.0.0.0:6002`. O log esperado é semelhante a:

```text
[HSM] Servidor TCP ativo em 0.0.0.0:6002
```

Ao iniciar, é o **`pswitch-comunicacao`** (não mais o `pswitch-app`) que conecta automaticamente ao HSM em `127.0.0.1:6002` — o mesmo vale pra Visa (`127.0.0.1:5000`, via `switch-simuladores`) e pro listener de POS (porta 9000): as três conexões são desse processo agora, nenhuma é do switch. Por isso o passo 2 (comunicação) idealmente sobe depois dos simuladores, embora não seja obrigatório: se um simulador ainda não estiver disponível, o cliente mantém o processo ativo e tenta reconectar a cada 5 segundos. O destino pode ser alterado antes de iniciar o `pswitch-comunicacao` (o mesmo `application.yml`/variáveis, agora lidos por esse processo em vez do `pswitch-app`):

```powershell
$env:PSWITCH_HSM_HOST = "127.0.0.1"
$env:PSWITCH_HSM_PORT = "6002"
$env:PSWITCH_HSM_RECONNECT_DELAY_MS = "5000"
```

Comandos de verdade são emitidos normalmente pelo switch durante uma transação, atravessando o
Redis entre os dois processos — cada bandeira/canal com seu par de filas:

- HSM: `hsm:pedidos` / `hsm:resposta:<header>` (o switch bloqueia esperando, por isso a correlação é
  por header).
- Visa: `visa:pedidos` / `visa:respostas` (fogo-e-esquece — o switch tem uma thread consumindo
  continuamente, correlacionando por terminalId+NSU).
- POS: `pos:pedidos` / `pos:respostas` (cada mensagem carrega `connectionId|payload em hex`, porque
  tem um terminal diferente por conexão).

Ver docs/arquitetura/topologia-implantacao.md.

Para executar com Docker:

```powershell
cd D:\adq\switch-simuladores
docker compose up --build
```

Para usar outra porta no PowerShell:

```powershell
$env:HSM_PORT = "7002"
npm run start:dev
```

O HSM não expõe um endpoint HTTP. A comunicação utiliza socket TCP persistente e cada mensagem recebe um prefixo binário de tamanho de 2 bytes, unsigned big-endian. O servidor devolve a resposta pela mesma conexão que originou o request.

O simulador implementa os comandos `SE → SF` (descriptografia simulada da trilha) e `G0 → G1` (tradução simulada do PIN block). Dados criptografados do comando `SE` terminados em `EE` provocam um atraso configurável, usado para testar timeout.

Para encerrar o simulador executado por npm, pressione `Ctrl+C`. No modo Docker, execute:

```powershell
docker compose down
```

## 5. Testar com o simulador POS

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
