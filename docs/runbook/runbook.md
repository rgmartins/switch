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
