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

Antes disso pode aparecer um erro de conexão com `localhost:27017` (MongoDB) — normal se não tiver um Mongo local rodando ainda; não derruba a aplicação.

## 3. Testar o recebimento TCP do POS

Com a aplicação em execução, abra outro PowerShell e envie uma mensagem para a porta `9000`:

```powershell
$client = [System.Net.Sockets.TcpClient]::new('127.0.0.1', 9000)
$payload = [System.Text.Encoding]::UTF8.GetBytes('OLA SWITCH')
$frame = [byte[]]::new($payload.Length + 2)
$frame[0] = 0x00
$frame[1] = 0x0A
[System.Array]::Copy($payload, 0, $frame, 2, $payload.Length)
$stream = $client.GetStream()
$stream.Write($frame, 0, $frame.Length)
$stream.Flush()

$responseHeader = [byte[]]::new(2)
[void]$stream.Read($responseHeader, 0, $responseHeader.Length)
$responseLength = ($responseHeader[0] -shl 8) -bor $responseHeader[1]
$responsePayload = [byte[]]::new($responseLength)
[void]$stream.Read($responsePayload, 0, $responsePayload.Length)
[System.Text.Encoding]::UTF8.GetString($responsePayload)

$client.Close()
```

No console da aplicação deverá aparecer:

```text
POS recebeu: OLA SWITCH
```

O cliente também deverá receber:

```text
recebi: OLA SWITCH, e estou dizendo que foi ok
```

Os dois primeiros bytes (`00 0A`) são um inteiro binário sem sinal, em ordem de rede (big-endian), e informam que o payload possui 10 bytes. O transporte aguarda o frame completo, remove esse prefixo e entrega ao módulo POS somente `OLA SWITCH`. A resposta recebe o mesmo framing e é enviada pela mesma conexão.
