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
