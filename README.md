# Lexo Backend (Java + Spring Boot)

Porte do **backend** do Lexo (originalmente Next.js + TypeScript) para **Java 21 + Spring Boot 3.4**.
Este módulo cobre o **núcleo**: domínio, autenticação/2FA e multi-tenancy. As integrações
externas (Stripe, Resend/email, IA) ficaram como *stubs* para uma fase seguinte.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.4 (Web, Data JPA, Security, Validation) |
| Banco | PostgreSQL (perfil padrão) — H2 em memória no perfil `dev` |
| Auth | JWT (jjwt, HS256) + BCrypt + TOTP (`dev.samstevens.totp`) |
| Build | Maven |

## O que foi portado

- **Domínio (10 entidades JPA)**: Organization, User, Client, Case (processo), Deadline (prazo),
  Invoice (honorário), UserInvite, AuditLog, ActivityLog, RateHit — espelhando o `schema.prisma`.
- **CRUD multi-tenant** de clientes, processos, agenda e financeiro. Toda query filtra por
  `organizationId`; nenhum dado vaza entre escritórios.
- **Autenticação**: registro de organização, login com BCrypt, **rate limiting** de login
  (10 tentativas / 15 min, persistido no banco), e **2FA/TOTP** com segredo cifrado em repouso
  (AES-256-GCM, portado de `lib/crypto.ts`).
- **Autorização no ponto de uso**: financeiro exige `ADMIN`/`ADVOGADO` (SECRETARIA barrada);
  gestão de equipe e auditoria exigem `ADMIN` — via `@PreAuthorize`, não só no roteamento.
- **Convites por email** com link temporário e aceite público.
- **Logs de auditoria e atividade**, **score de risco de prazo** (`lib/risk.ts`),
  **validação de CPF/CNPJ** (`lib/document.ts`) e **cron de notificação de prazos**.

## Stubs (a implementar na fase 2)

- `EmailService` — apenas registra em log (substituir por client da Resend / JavaMailSender).
- Stripe (billing) e funcionalidades de IA (minutas, resumo, extração de PDF, pesquisa jurídica).

## Como rodar

### Modo rápido (sem banco — H2 em memória)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Sobe em `http://localhost:8080`. Console do H2 em `/h2-console`.

### Com PostgreSQL

```bash
# variáveis de ambiente (ou edite application.yml)
export DATABASE_URL="jdbc:postgresql://localhost:5432/lexo_dev"
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export AUTH_SECRET="uma-string-aleatoria-com-no-minimo-32-bytes"

mvn spring-boot:run
```

## Endpoints principais

| Método | Rota | Acesso |
|---|---|---|
| POST | `/api/auth/register` | público |
| POST | `/api/auth/login` | público |
| GET/POST/PUT/DELETE | `/api/clientes` | autenticado |
| GET/POST/PUT/DELETE | `/api/processos` | autenticado |
| GET/POST/PUT/DELETE + PATCH `/status` | `/api/agenda` | autenticado |
| GET/POST/PUT/DELETE + `/relatorio` | `/api/financeiro` | ADMIN/ADVOGADO |
| GET/POST/DELETE | `/api/usuarios`, `/api/usuarios/convites` | ADMIN |
| GET `/info/{token}`, POST `/aceitar` | `/api/convites` | público |
| POST | `/api/2fa/iniciar` `/confirmar` `/desativar` | autenticado |
| GET | `/api/auditoria` | ADMIN |
| POST | `/api/cron/notify-deadlines` | header `Authorization: Bearer $CRON_SECRET` |

### Autenticação

Login retorna `{ "token": "...", "user": {...} }`. Envie o token nas demais chamadas:

```
Authorization: Bearer <token>
```

## Variáveis de ambiente

| Var | Default | Uso |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/lexo_dev` | conexão JDBC |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | credenciais |
| `AUTH_SECRET` | (placeholder) | assinatura do JWT e fallback da cifra TOTP (≥32 bytes) |
| `TOTP_ENC_KEY` | — | chave dedicada da cifra TOTP (opcional) |
| `JWT_EXP_MINUTES` | `720` | validade do token |
| `BASE_URL` | `http://localhost:8080` | base dos links de convite |
| `CRON_SECRET` | — | protege o disparo manual do cron |
