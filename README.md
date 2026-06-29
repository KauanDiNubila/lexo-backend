# Lexo — Backend em Microserviços (Java + Spring Boot)

Sistema de gestão para escritórios de advocacia (SaaS multi-tenant), construído como uma
**arquitetura de microserviços** com Java 21, Spring Boot 3.4 e Spring Cloud.

O projeto nasceu como um monólito (porte de um backend Next.js/TypeScript) e foi
**decomposto incrementalmente** (estratégia *strangler*) até **zerar o monólito** — hoje
cada domínio é um serviço independente, com seu próprio banco, comunicando-se de forma
síncrona (Feign) e assíncrona (Kafka e RabbitMQ).

---

## Arquitetura

```
                              ┌─────────────────┐
            Cliente / Front ─▶│   API Gateway   │  valida JWT, injeta identidade (X-User-*)
                              │   (porta 8080)  │
                              └────────┬────────┘
                                       │  descobre serviços via Eureka (8761)
   ┌──────────┬──────────┬────────────┼────────────┬──────────────┬──────────────┐
   ▼          ▼          ▼            ▼            ▼              ▼              ▼
┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌─────────────┐
│  auth  │ │cliente │ │auditoria │ │ processo │ │financeiro │ │ notificacao │
│  8082  │ │  8083  │ │  8084    │ │  8086    │ │  8081     │ │   8085      │
│ db5434 │ │ db5435 │ │ db5436   │ │ db5437   │ │ db5438    │ │ (sem banco) │
└───┬────┘ └───┬────┘ └────▲─────┘ └────┬─────┘ └─────┬─────┘ └──────▲──────┘
    │          │           │            │             │              │
    │          │     consome eventos    │ publica     │ Feign        │ consome
    │          └── publica ──┐          │ eventos     │ (cliente +   │ fila de
    │              eventos   ▼          ▼             │  processo)   │ e-mails
    │                   ┌─────────────────┐           │              │
    │                   │      KAFKA       │           │              │
    │                   └─────────────────┘           │              │
    └──── Feign ───┐  ┌──── Feign ────────────────────┘   ┌──────────┘
   (responsavelId) ▼  ▼  (clientId / caseId)              │ RABBITMQ
                 (validação síncrona)              ┌───────┴────────┐
                                                   │   fila e-mails  │
                                                   └─────────────────┘
```

### Serviços

| Serviço | Porta | Banco | Responsabilidade |
|---------|-------|-------|------------------|
| **discovery-server** | 8761 | — | Eureka: registro e descoberta de serviços |
| **api-gateway** | 8080 | — | Porta de entrada única; valida JWT e roteia |
| **auth-service** | 8082 | `lexo-auth-db` (5434) | Autenticação, usuários, organizações, 2FA, equipe |
| **cliente-service** | 8083 | `lexo-cliente-db` (5435) | Gestão de clientes (CPF/CNPJ) |
| **processo-service** | 8086 | `lexo-processo-db` (5437) | Processos, agenda (prazos) e atividades |
| **financeiro-service** | 8081 | `lexo-financeiro-db` (5438) | Honorários |
| **auditoria-service** | 8084 | `lexo-auditoria-db` (5436) | Log de auditoria (consome eventos do Kafka) |
| **notificacao-service** | 8085 | — | Envio de e-mails (consome fila do RabbitMQ) |

---

## Padrões de comunicação

| Padrão | Tecnologia | Onde |
|--------|-----------|------|
| **Síncrono** | OpenFeign (REST + load balancing via Eureka) | processo-service → cliente-service (`clientId`) e → auth-service (`responsavelId`); financeiro-service → cliente-service (`clientId`) e → processo-service (`caseId`) |
| **Eventos de domínio** | Apache Kafka | cliente-service e processo-service **publicam** fatos (`CLIENTE_CRIADO`, `PROCESSO_CRIADO`); auditoria-service **consome** |
| **Filas de tarefas** | RabbitMQ (com retry + dead-letter queue) | auth-service e processo-service **enfileiram** e-mails; notificacao-service **consome** |
| **Cache** | Redis | cache de leitura (chave por tenant) + rate limiting de login |

---

## Segurança distribuída

- O **gateway** é o único ponto que valida o **JWT** (HS256, segredo compartilhado).
- Após validar, injeta a identidade em headers de confiança: `X-User-Id`, `X-Org-Id`,
  `X-User-Role`, `X-User-Name`, `X-User-Email`.
- Os serviços **confiam nesses headers** (via `HeaderAuthenticationFilter`) — não revalidam o token.
- **Anti-spoofing**: o gateway **remove** quaisquer headers `X-User-*` enviados pelo cliente,
  impedindo que alguém se passe por outro usuário.
- **Multi-tenancy**: toda query filtra por `organizationId` — um escritório nunca vê dados de outro.

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.4 |
| Microserviços | Spring Cloud 2024.0.0 (Eureka, Gateway, OpenFeign, LoadBalancer) |
| Bancos | PostgreSQL (um por serviço) |
| Cache | Redis |
| Mensageria | Apache Kafka (KRaft) + RabbitMQ |
| Segurança | JWT (jjwt) + BCrypt + TOTP (2FA) |
| Build | Maven (multi-módulo) |
| Testes | JUnit 5 + MockMvc + H2 (em memória) |

---

## Como rodar

### 1. Suba a infraestrutura (Docker)

```bash
docker compose up -d
```

Sobe: 5 bancos PostgreSQL, Redis, Kafka e RabbitMQ.

> **Observação:** ao subir vários containers de uma vez, um banco pode ficar no estado
> "Created" sem iniciar. Se acontecer, rode `docker compose up -d --force-recreate <nome-do-db>`.

### 2. Compile

```bash
mvn clean package
```

### 3. Suba os serviços (o Eureka primeiro)

```bash
java -jar discovery-server/target/discovery-server-0.1.0.jar       # 8761 (primeiro!)
java -jar api-gateway/target/api-gateway-0.1.0.jar                 # 8080
java -jar auth-service/target/auth-service-0.1.0.jar               # 8082
java -jar cliente-service/target/cliente-service-0.1.0.jar         # 8083
java -jar processo-service/target/processo-service-0.1.0.jar       # 8086
java -jar financeiro-service/target/financeiro-service-0.1.0.jar   # 8081
java -jar auditoria-service/target/auditoria-service-0.1.0.jar     # 8084
java -jar notificacao-service/target/notificacao-service-0.1.0.jar # 8085
```

> Pelo IntelliJ, basta abrir o `pom.xml` da raiz e dar *Run* em cada classe `*Application`.
> Após subir, o registro no Eureka leva ~30s para propagar (heartbeat + fetch).

### 4. Use a API (tudo pelo gateway, porta 8080)

```bash
# Registrar (rota pública)
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"organizationName":"Meu Escritório","name":"Ana","email":"ana@x.com",
       "password":"senha12345","confirmPassword":"senha12345"}'

# Usar o token retornado nas rotas protegidas
curl http://localhost:8080/api/clientes -H "Authorization: Bearer <TOKEN>"
```

| Recurso | Rota | Vai para |
|---------|------|----------|
| Auth / usuários / convites / 2FA | `/api/auth`, `/api/usuarios`, `/api/convites`, `/api/2fa` | auth-service |
| Clientes | `/api/clientes` | cliente-service |
| Processos / agenda | `/api/processos`, `/api/agenda` | processo-service |
| Honorários | `/api/financeiro` | financeiro-service |
| Auditoria | `/api/auditoria` | auditoria-service |

Painéis úteis: **Eureka** em http://localhost:8761 · **RabbitMQ** em http://localhost:15673
(guest/guest).

---

## Portas (referência rápida)

| Porta | O quê |
|-------|-------|
| 8080 | API Gateway (entrada única) |
| 8081–8086 | Serviços (financeiro, auth, cliente, auditoria, notificacao, processo) |
| 8761 | Eureka |
| 5434–5438 | Bancos PostgreSQL (um por serviço) |
| 6380 | Redis |
| 9094 | Kafka |
| 5673 / 15673 | RabbitMQ / painel |

---

## Testes

```bash
mvn test
```

Testes unitários (validação CPF/CNPJ, risco de prazo, JWT) e de integração (CRUD,
multi-tenancy) rodam sobre **H2 em memória** — sem Docker.

---

## Estrutura do repositório

```
lexo-backend/
├── pom.xml                 # POM pai (multi-módulo + BOM Spring Cloud)
├── docker-compose.yml      # infraestrutura (bancos, Redis, Kafka, RabbitMQ)
├── discovery-server/       # Eureka
├── api-gateway/            # Spring Cloud Gateway + validação de JWT
├── auth-service/           # autenticação, usuários, 2FA
├── cliente-service/        # clientes
├── processo-service/       # processos, agenda, atividades
├── financeiro-service/     # honorários
├── auditoria-service/      # auditoria (event-driven)
└── notificacao-service/    # e-mails (consumidor RabbitMQ)
```

## Status / próximos passos

- ✅ **Decomposição completa**: todos os domínios são microserviços (sem monólito).
- 🔭 Evoluções possíveis: tracing distribuído (Zipkin/Micrometer), resiliência
  (Resilience4j — circuit breaker), config server centralizado, autenticação
  serviço-a-serviço nos endpoints `/internal/**`, e uma biblioteca compartilhada para o
  código de segurança comum (`AuthUser`, `HeaderAuthenticationFilter`, etc.).
