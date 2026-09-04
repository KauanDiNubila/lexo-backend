# Deploy em produção

A stack de produção é o `docker-compose.prod.yml`. Ele é um arquivo **independente** do
`docker-compose.yml` de desenvolvimento, não um override — em Compose, listas como `ports` são
somadas quando se combinam arquivos, então um override não conseguiria *fechar* as portas que o
arquivo de dev publica. Rodar prod é sempre `-f docker-compose.prod.yml`, nunca os dois juntos.

## Diferenças em relação ao ambiente de desenvolvimento

| | dev | prod |
|---|---|---|
| Portas publicadas no host | 15 (5 Postgres, Redis, Kafka, RabbitMQ, Zipkin, Mailpit, Eureka, gateway, frontend) | 3 (Caddy: 80, 443, 443/udp) |
| Segredos | valores default no arquivo | obrigatórios via `.env.prod`, o Compose recusa subir sem eles |
| Profile Spring | `dev` | `prod` — os serviços **se recusam a iniciar** com a chave de assinatura default |
| Credenciais de banco | `postgres/postgres` | definidas por você |
| Redis / RabbitMQ | sem senha | com senha |
| TLS | não | Caddy com Let's Encrypt automático |
| Memória | sem limite | limite por container |

Bancos, filas, cache, discovery e os próprios serviços não expõem porta nenhuma no host: só
conversam pela rede interna do Compose. O único ponto de entrada é o Caddy.

## Preparação

1. Aponte o DNS do seu domínio (registro A) para o IP da máquina.

2. Crie o `.env.prod` a partir do exemplo:

```bash
cp .env.prod.example .env.prod
```

3. Gere os segredos. `AUTH_SECRET` e `GATEWAY_SIGNING_KEY` precisam de no mínimo 32 bytes —
   os comandos abaixo geram bem mais que isso:

```bash
python3 -c "import secrets; print('POSTGRES_PASSWORD=' + secrets.token_urlsafe(24))"
python3 -c "import secrets; print('REDIS_PASSWORD=' + secrets.token_urlsafe(24))"
python3 -c "import secrets; print('RABBIT_PASSWORD=' + secrets.token_urlsafe(24))"
python3 -c "import secrets; print('AUTH_SECRET=' + secrets.token_urlsafe(48))"
python3 -c "import secrets; print('GATEWAY_SIGNING_KEY=' + secrets.token_urlsafe(48))"
python3 -c "import secrets; print('LEXO_INTERNAL_KEY=' + secrets.token_urlsafe(32))"
```

Preencha também `APP_DOMAIN` (sem `https://`) e `ACME_EMAIL` (usado pelo Let's Encrypt para
avisar sobre renovação). `.env.prod` está no `.gitignore` e não deve ser commitado.

`GEMINI_API_KEY` é opcional: sem ela, a Lexo IA sobe normalmente e responde em modo mock (heurístico,
sem custo). Para respostas reais, gere uma chave gratuita em https://aistudio.google.com/apikey e
preencha essa variável — veja o item de rate limiting em "Pontos em aberto" antes de habilitar em
produção pública.

## Subir

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

A primeira subida leva alguns minutos: compila os 9 serviços Java e o Caddy emite o certificado.
Acompanhe com `docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f caddy`.

## Firewall

Fechar as portas no Compose resolve o que o Docker publica, mas o Docker manipula iptables
diretamente e pode furar regras de UFW. Na VPS, libere apenas o necessário no firewall do
provedor (Security List, na Oracle Cloud):

- 22/tcp (SSH)
- 80/tcp e 443/tcp (Caddy)
- 443/udp (HTTP/3, opcional)

Nada mais precisa estar aberto.

## Verificação pós-deploy

```bash
curl -sI https://SEU_DOMINIO | head -20
```

Deve retornar `200`, `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options` e
`Referrer-Policy`. Confirme também que nenhuma porta de infraestrutura respondeu:

```bash
for p in 5434 5435 5436 5437 5438 6380 9094 15673 8761 8025 8090; do
  timeout 3 bash -c "</dev/tcp/SEU_DOMINIO/$p" 2>/dev/null && echo "ABERTA: $p" || echo "fechada: $p"
done
```

Todas devem aparecer como fechadas.

## Pontos em aberto

- **CSP**: o `nginx.conf` ainda não define `Content-Security-Policy`. Como front e API ficam no
  mesmo domínio via Caddy, dá para usar uma política restritiva com `connect-src 'self'`. Falta
  validar que ela não quebra o canvas WebGL da landing nem os estilos inline que sobraram.
- **E-mail**: o Mailpit captura as mensagens em vez de entregá-las. Para convites chegarem a
  destinatários reais é preciso apontar `LEXO_SMTP_HOST`/`LEXO_SMTP_PORT` para um SMTP de verdade.
- **Rate limiting**: os endpoints `/api/ia/**` já têm limite por organização (`LEXO_IA_LIMITE`, 20
  requisições por minuto por padrão, contadas no Redis). O que falta é teto de consumo acumulado —
  o limite por minuto contém rajada, mas não impede um gasto alto ao longo do mês.
- **Backup**: os volumes Postgres não têm rotina de backup.
