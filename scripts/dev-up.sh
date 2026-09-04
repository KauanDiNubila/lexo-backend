#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "== subindo backend (docker) =="
docker compose up -d

echo
echo "== aguardando o gateway responder =="
for i in $(seq 1 30); do
  if curl -sf http://localhost:8090/actuator/health >/dev/null 2>&1; then
    echo "gateway ok."
    break
  fi
  sleep 4
done

echo
echo "== status dos containers =="
docker compose ps --format "table {{.Service}}\t{{.State}}"

echo
echo "Backend pronto em http://localhost:8090"
echo "Agora suba o frontend em outro terminal:"
echo "  cd frontend && npm run dev"
echo
echo "App em http://localhost:5180"
echo "Na primeira vez, crie um escritorio em /login?modo=registro."
