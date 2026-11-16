# ADR 0013: Limite por cliente atrás do proxy e identidade completa no gateway

- Status: Proposto
- Data: 2026-11-17

## Contexto

Duas coisas no gateway não estavam erradas por acidente, mas faziam menos do que o nome dizia.

**O limite de requisições contava o endereço do proxy.** Em produção o gateway só recebe tráfego do Caddy (`reverse_proxy api-gateway:8090`), então o endereço remoto de toda requisição era o do Caddy. O balde de 30 requisições por segundo e 60 de rajada era um só para todos os usuários: um cliente que fizesse uma rajada deixava os outros em 429, e quem quisesse tirar o serviço do ar só precisava gastar o balde. O limite protegia pouco e punia todos.

**Um token assinado, mas sem usuário, escritório ou papel, passava.** O auth-service sempre emite essas três declarações, então só um token assinado com o segredo poderia vir sem elas. Mesmo assim, o gateway o encaminhava, assinando cabeçalhos de identidade incompletos, e quem fecha a decisão é cada serviço de destino. A pergunta "quem é este usuário" deve ser respondida inteira no gateway ou negada.

## Decisão

**O cliente é o endereço que o proxy acrescentou.** `RateLimitFilter` usa o `XForwardedRemoteAddressResolver` do Spring Cloud Gateway com `lexo.rate-limit.trusted-proxies` (variável `LEXO_RATE_LIMIT_TRUSTED_PROXIES`). Com um proxy confiável, vale a última entrada de `X-Forwarded-For`, a que o Caddy escreve com o endereço real da conexão. Entradas anteriores, que o cliente pode inventar, são ignoradas. Sem cabeçalho (acesso direto, desenvolvimento) vale o endereço remoto.

**Seguro por omissão.** O padrão é `0`: o cabeçalho é ignorado. Só o `docker-compose.prod.yml` liga `1`, onde o Caddy existe. Esquecer de ligar em produção devolve o comportamento antigo (um balde para todos), que é visível e inofensivo; confiar por omissão deixaria qualquer gateway exposto à mercê de um cabeçalho forjado. Dois testes leem o compose e as propriedades para que isso não se perca.

**O Caddy sobrescreve o cabeçalho de forma explícita.** O `Caddyfile` usa `header_up X-Forwarded-For {remote_host}`. Conferi com um Caddy 2.11.4 de verdade e um serviço que devolve os cabeçalhos recebidos: mesmo sem essa linha o Caddy já descartava o `X-Forwarded-For` forjado pelo cliente e escrevia só o endereço da conexão. A linha não corrige um defeito; fixa na nossa configuração o que antes dependia do padrão de uma imagem com tag flutuante (`caddy:2-alpine`), e garante que o valor é sempre um IP literal. Isso importa porque o resolvedor faz `new InetSocketAddress(String, int)` com o valor recebido, que tentaria resolver DNS (bloqueante) se viesse um nome.

**Uma identidade só existe completa.** `Identity` recusa usuário, escritório ou papel nulos ou em branco. `JjwtTokenVerifier` já tratava `IllegalArgumentException` como token inválido, então o token incompleto vira 401 sem nenhum desvio novo.

## Consequências

- Com `trusted-proxies=1` o gateway confia no último valor de `X-Forwarded-For`. Isso só é seguro enquanto o gateway não estiver exposto sem o Caddy na frente: um cliente que falasse direto com a porta 8090 poderia escolher o próprio endereço. Em produção a porta não é publicada (um teste confere o compose); no desenvolvimento ela é, e lá o padrão `0` ignora o cabeçalho.
- Se um dia houver outro proxy na frente do Caddy (CDN, balanceador), o valor de `trusted-proxies` precisa subir, ou todos os clientes voltam a parecer um só.
- A documentação OpenAPI (`/docs/`) continua pública, inclusive em produção. Não foi alterado aqui; é uma decisão de exposição, não de código.

## Referências

- ADR 0012, gateway em hexagonal mantendo o fluxo reativo.
