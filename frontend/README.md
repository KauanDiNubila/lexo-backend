# Lexo Web — Frontend

SPA (React + Vite + TypeScript) que consome a API de microserviços do Lexo através do
**API Gateway** (`http://localhost:8080`). Tema escuro próprio.

## Telas

| Tela | Microserviço consumido |
|------|------------------------|
| Login / Registro | auth-service |
| Visão geral (dashboard) | (clientes) |
| Clientes | cliente-service |
| Processos | processo-service |
| Agenda (prazos) | processo-service |
| Financeiro (honorários) | financeiro-service |

## Como rodar

```bash
# 1. suba o backend (na raiz do repo): docker compose up -d + os servicos
# 2. no frontend:
cd frontend
npm install
npm run dev          # http://localhost:5173
```

A URL do gateway vem de `VITE_GATEWAY_URL` (`.env`), com fallback para `http://localhost:8080`.

## Arquitetura da integração

```
navegador → API Gateway (valida JWT, CORS) → microservicos
```

O token JWT (emitido pelo auth-service) fica no `localStorage` e é enviado como
`Authorization: Bearer` em cada chamada. O gateway valida e injeta a identidade nos serviços.
