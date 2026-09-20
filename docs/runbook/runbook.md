# Runbook — como subir o ambiente

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
