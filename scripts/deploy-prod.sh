#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env.prod ]; then
  echo "Faltando .env.prod. Copie o exemplo e preencha:"
  echo "  cp .env.prod.example .env.prod"
  echo
  echo "Gere os segredos com:"
  cat <<'EOF'
  python3 -c "import secrets; print('POSTGRES_PASSWORD=' + secrets.token_urlsafe(24))"
  python3 -c "import secrets; print('REDIS_PASSWORD=' + secrets.token_urlsafe(24))"
  python3 -c "import secrets; print('RABBIT_PASSWORD=' + secrets.token_urlsafe(24))"
  python3 -c "import secrets; print('AUTH_SECRET=' + secrets.token_urlsafe(48))"
  python3 -c "import secrets; print('GATEWAY_SIGNING_KEY=' + secrets.token_urlsafe(48))"
  python3 -c "import secrets; print('LEXO_INTERNAL_KEY=' + secrets.token_urlsafe(32))"
EOF
  echo
  echo "Preencha tambem APP_DOMAIN (sem https://) e ACME_EMAIL, e rode este script de novo."
  exit 1
fi

echo "== subindo stack de producao =="
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build

echo
echo "== aguardando o Caddy responder =="
for i in $(seq 1 30); do
  if curl -sk -o /dev/null https://localhost/ 2>/dev/null; then
    echo "Caddy respondendo."
    break
  fi
  sleep 5
done

echo
echo "== status dos servicos =="
docker compose --env-file .env.prod -f docker-compose.prod.yml ps --format "table {{.Service}}\t{{.State}}"

echo
echo "== portas publicadas no host (deve ser so 80/443) =="
docker compose --env-file .env.prod -f docker-compose.prod.yml config | grep -A1 "published:" || true

echo
echo "Pronto. Acesse https://\$APP_DOMAIN (definido no .env.prod)."
echo "Logs ao vivo: docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f"
