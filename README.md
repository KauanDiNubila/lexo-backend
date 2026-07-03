# ⚖️ Lexo — Plataforma Jurídica em Microserviços (Full-Stack + IA)

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F)
![React](https://img.shields.io/badge/React-Vite%20%2B%20TS-61DAFB)
![Gemini](https://img.shields.io/badge/IA-Google%20Gemini-8E75FF)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

**SaaS multi-tenant de gestão para escritórios de advocacia**, construído como uma **arquitetura de microserviços** com Java 21, Spring Boot 3.4 e Spring Cloud — com **frontend em React**, **portal do cliente** e recursos de **IA de verdade** (Google Gemini).

O projeto nasceu como um monólito (porte de um backend Next.js/TypeScript) e foi **decomposto incrementalmente** (estratégia *strangler*) até **zerar o monólito**: hoje cada domínio é um serviço independente, com seu próprio banco, comunicando-se de forma síncrona (Feign) e assíncrona (Kafka e RabbitMQ).

---

## ✨ Destaques

- **8 microserviços** com service discovery (Eureka), API Gateway e banco-por-serviço (PostgreSQL).
- **Segurança distribuída**: o gateway valida o JWT e injeta identidade em headers de confiança; anti-spoofing e isolamento multi-tenant em toda query.
- **Resiliência**: *circuit breaker* (Resilience4j) com *fallback* nas chamadas entre serviços — a queda de um serviço não derruba os outros.
- **Observabilidade**: *tracing* distribuído (Micrometer + Zipkin) — uma requisição é rastreável de ponta a ponta, incluindo os saltos Feign.
- **Mensageria**: eventos de domínio (Kafka) para auditoria *event-driven* e fila de e-mails (RabbitMQ) com *retry* + *dead-letter queue*.
- **🤖 Lexo IA (real)**: resumo de processo, assistente jurídico (chat) e rascunho de petição via **Google Gemini**, num microserviço dedicado — com *fallback* heurístico que roda **sem chave e custo zero**.
- **🔗 Portal do cliente**: link público (*magic link*, sem login) onde o cliente acompanha processos, prazos, andamentos e financeiro em tempo real.
- **Frontend completo** (React + Vite + TypeScript): landing page, app do escritório e portal, no mesmo repositório.

---

## 🏗️ Arquitetura

```
                        ┌───────────────────────────┐
     Frontend (React) ─▶│        API Gateway         │  valida JWT · injeta X-User-* · roteia
     Portal do cliente  │        (porta 8080)        │
                        └─────────────┬──────────────┘
                                      │  descobre serviços via Eureka (8761)
   ┌──────────┬──────────┬───────────┼───────────┬────────────┬───────────┬──────────┐
   ▼          ▼          ▼           ▼           ▼            ▼           ▼          ▼
┌───────┐ ┌───────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌───────────┐ ┌────────┐  │
│ auth  │ │cliente│ │auditoria│ │processo │ │financeiro│ │notificacao│ │   ia   │  │
│ 8082  │ │ 8083  │ │  8084   │ │  8086   │ │  8081    │ │   8085    │ │  8087  │  │
└───┬───┘ └───┬───┘ └────▲────┘ └────┬────┘ └────┬─────┘ └─────▲─────┘ └───┬────┘  │
    │         │          │           │           │             │           │       │
    │         │   consome eventos    │ publica   │  Feign       │ consome   │ Google│
    │         └─ publica ─┐          │ eventos   │ (cliente +   │ fila de   │ Gemini│
    │            eventos  ▼          ▼           │  processo)   │ e-mails   ▼       │
    │                ┌─────────┐                 │              │       (free tier) │
    │                │  KAFKA  │                 │              │                   │
    │                └─────────┘                 │              │                   │
    └──── Feign ──────┐  ┌──── Feign ────────────┘        ┌─────┴─────┐             │
   (responsavelId)    ▼  ▼  (clientId / caseId)           │ RABBITMQ  │             │
                  (validação síncrona + circuit breaker)  │fila e-mails│            │
                                                          └───────────┘             │
   Portal do cliente ── cliente-service agrega (Feign) ── processo + financeiro ────┘
```

### Serviços

| Serviço | Porta | Banco | Responsabilidade |
|---------|-------|-------|------------------|
| **discovery-server** | 8761 | — | Eureka: registro e descoberta de serviços |
| **api-gateway** | 8080 | — | Porta de entrada única; valida JWT e roteia |
| **auth-service** | 8082 | `lexo-auth-db` (5434) | Autenticação, usuários, organizações, 2FA, equipe |
| **cliente-service** | 8083 | `lexo-cliente-db` (5435) | Clientes (CPF/CNPJ) + agregação do portal do cliente |
| **processo-service** | 8086 | `lexo-processo-db` (5437) | Processos, agenda (prazos), andamentos e atividades |
| **financeiro-service** | 8081 | `lexo-financeiro-db` (5438) | Honorários |
| **auditoria-service** | 8084 | `lexo-auditoria-db` (5436) | Log de auditoria (consome eventos do Kafka) |
| **notificacao-service** | 8085 | — | Envio de e-mails (consome fila do RabbitMQ) |
| **ia-service** | 8087 | — | Recursos de IA (resumo, chat, petição) via Google Gemini |

---

## 🔀 Padrões de comunicação

| Padrão | Tecnologia | Onde |
|--------|-----------|------|
| **Síncrono** | OpenFeign + Resilience4j (circuit breaker) | processo → cliente/auth; financeiro → cliente/processo; cliente → processo/financeiro (portal) |
| **Eventos de domínio** | Apache Kafka | cliente e processo **publicam** (`CLIENTE_CRIADO`, `PROCESSO_CRIADO`); auditoria **consome** |
| **Filas de tarefas** | RabbitMQ (retry + dead-letter queue) | auth e processo **enfileiram** e-mails; notificacao **consome** |
| **Cache** | Redis | cache de leitura (chave por tenant) + rate limiting de login |
| **Autenticação interna** | Header `X-Internal-Key` | protege os endpoints `/internal/**` (serviço-a-serviço) |

---

## 🔒 Segurança distribuída

- O **gateway** é o único ponto que valida o **JWT** (HS256, segredo compartilhado).
- Após validar, injeta a identidade em headers de confiança: `X-User-Id`, `X-Org-Id`, `X-User-Role`, `X-User-Name`, `X-User-Email`.
- Os serviços **confiam nesses headers** (via `HeaderAuthenticationFilter`) — não revalidam o token.
- **Anti-spoofing**: o gateway **remove** quaisquer headers `X-User-*` enviados pelo cliente.
- **Multi-tenancy**: toda query filtra por `organizationId` — um escritório nunca vê dados de outro.
- **Serviço-a-serviço**: os endpoints `/internal/**` exigem uma chave interna (`X-Internal-Key`).
- **Fail-fast de produção**: o gateway e o auth-service **recusam iniciar** sob o profile `prod` se o segredo do JWT ainda for o default versionado.

---

## 🤖 Lexo IA

Um microserviço dedicado (`ia-service`) que integra o **Google Gemini** (free tier). Sem chave configurada, ele responde com um *fallback* heurístico — o produto **funciona e demonstra a arquitetura com custo zero**, e "liga" a IA real ao definir a variável `GEMINI_API_KEY`.

| Recurso | Rota | O que faz |
|---------|------|-----------|
| **Resumir processo** | `POST /api/ia/resumir-processo` | Resume o processo (status + prazos) para o advogado |
| **Assistente jurídico** | `POST /api/ia/chat` | Chat multi-turno, com contexto opcional de processo |
| **Rascunho de petição** | `POST /api/ia/rascunhar-peticao` | Gera a minuta de uma peça, com placeholders para revisão |

> Chave gratuita em [aistudio.google.com](https://aistudio.google.com). Modelo padrão: `gemini-2.5-flash`.

---

## 🖥️ Frontend (`/frontend`)

SPA em **React + Vite + TypeScript** que consome o backend via gateway:

- **Landing page** pública e **app do escritório** (dashboard com KPIs reais, clientes, processos, agenda, financeiro, equipe, auditoria, configurações).
- **Assistente** e **Petições** com IA; respostas renderizadas em markdown.
- **Portal do cliente**: página pública `/portal/:token` onde o cliente acompanha processos, prazos, **andamentos** e situação financeira — sem login.

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
```

---

## 🧰 Stack

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21 · Spring Boot 3.4 · Spring Cloud 2024 (Eureka, Gateway, OpenFeign, LoadBalancer) |
| Resiliência / Observabilidade | Resilience4j (circuit breaker) · Micrometer Tracing + Zipkin |
| Bancos | PostgreSQL (um por serviço) · Redis (cache/rate limit) |
| Mensageria | Apache Kafka (KRaft) · RabbitMQ (retry + DLQ) |
| Segurança | JWT (jjwt) · BCrypt · TOTP (2FA) |
| IA | Google Gemini (free tier) com fallback heurístico |
| Frontend | React · Vite · TypeScript · Tailwind CSS · lucide-react |
| Build / Testes | Maven (multi-módulo) · JUnit 5 · MockMvc · H2 |

---

## ▶️ Como rodar

### Atalho (Windows): sobe tudo com um comando

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1   # infra + Eureka + serviços + gateway + frontend
powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1    # derruba os processos (mantém o Docker)
```

O script usa o JDK 21, sobe o Eureka primeiro (espera responder) e desanexa cada processo. Logs em `scripts/logs/`. Requer os jars já compilados (passo 2).

### 1. Infraestrutura (Docker)

```bash
docker compose up -d      # PostgreSQL (x5), Redis, Kafka, RabbitMQ, Zipkin, Mailpit
```

### 2. Compile

```bash
mvn clean package
```

### 3. Suba os serviços (Eureka primeiro)

```bash
java -jar discovery-server/target/discovery-server-0.1.0.jar       # 8761 (primeiro!)
java -jar api-gateway/target/api-gateway-0.1.0.jar                 # 8080
java -jar auth-service/target/auth-service-0.1.0.jar               # 8082
java -jar cliente-service/target/cliente-service-0.1.0.jar         # 8083
java -jar processo-service/target/processo-service-0.1.0.jar       # 8086
java -jar financeiro-service/target/financeiro-service-0.1.0.jar   # 8081
java -jar auditoria-service/target/auditoria-service-0.1.0.jar     # 8084
java -jar notificacao-service/target/notificacao-service-0.1.0.jar # 8085
java -jar ia-service/target/ia-service-0.1.0.jar                   # 8087
```

### 4. (Opcional) Ligue a IA real

```bash
# Windows (variável de ambiente do usuário) — depois reinicie o ia-service:
setx GEMINI_API_KEY "sua-chave-do-aistudio"
```

Sem a chave, a Lexo IA roda em modo *fallback* (mock) — nada quebra.

### 5. Use a API (tudo pelo gateway, porta 8080)

```bash
# Registrar (rota pública)
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"organizationName":"Meu Escritorio","name":"Ana","email":"ana@x.com",
       "password":"senha12345","confirmPassword":"senha12345"}'

# Usar o token retornado nas rotas protegidas
curl http://localhost:8080/api/clientes -H "Authorization: Bearer <TOKEN>"
```

Painéis úteis: **Eureka** (8761) · **Zipkin** (9411) · **Mailpit** (8025) · **RabbitMQ** (15673, guest/guest).

---

## 🗺️ Portas

| Porta | O quê |
|-------|-------|
| 8080 | API Gateway (entrada única) |
| 8081–8087 | Serviços (financeiro, auth, cliente, auditoria, notificacao, processo, ia) |
| 8761 | Eureka |
| 5173 | Frontend (Vite) |
| 5434–5438 | PostgreSQL (um por serviço) |
| 6380 | Redis · 9094 Kafka · 5673/15673 RabbitMQ · 9411 Zipkin · 1025/8025 Mailpit |

---

## 🧪 Testes

```bash
mvn test
```

Testes unitários (validação CPF/CNPJ, risco de prazo, JWT) e de integração (CRUD, multi-tenancy) rodam sobre **H2 em memória** — sem Docker.

---

## 📁 Estrutura

```
lexo-backend/
├── pom.xml                 # POM pai (multi-módulo + BOM Spring Cloud)
├── docker-compose.yml      # infraestrutura
├── scripts/                # start-all / stop-all (Windows)
├── discovery-server/       # Eureka
├── api-gateway/            # Spring Cloud Gateway + validação de JWT
├── auth-service/           # autenticação, usuários, 2FA
├── cliente-service/        # clientes + portal do cliente
├── processo-service/       # processos, agenda, andamentos
├── financeiro-service/     # honorários
├── auditoria-service/      # auditoria (event-driven)
├── notificacao-service/    # e-mails (consumidor RabbitMQ)
├── ia-service/             # IA (Google Gemini)
└── frontend/               # SPA React + Vite + TypeScript
```

---

## 🛣️ Roadmap

- Config Server centralizado e biblioteca compartilhada para o código de segurança comum.
- Captura de publicações (DJe) → geração automática de prazos.
- Jurimetria (relatórios/BI) e integração de cobrança (PIX/boleto).
