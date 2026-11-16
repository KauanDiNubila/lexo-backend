# ADR 0012: Gateway em hexagonal, mantendo o fluxo reativo

- Status: Proposto
- Data: 2026-11-16

## Contexto

O api-gateway tinha quatro classes soltas em `app.lexo.gateway`. `AutenticacaoGatewayFilter` fazia tudo de uma vez: lia o cabeçalho, verificava o JWT com a biblioteca jjwt, decidia o que é rota pública, consultava o Redis para saber se a sessão foi cortada, assinava os cabeçalhos de identidade e respondia 401. `LimiteTaxaGatewayFilter` juntava a decisão de limitar com a escolha da chave (o IP do cliente). O código de negociação (rotas, CORS) já estava em `application.properties` e em `CorsConfig`.

Os testes existentes montavam os filtros pelo construtor, então qualquer mudança de classe os quebrava. O serviço também não tinha nenhum teste com Redis ou com um destino de verdade.

## Decisão

**As regras saem do filtro.** O domínio ficou com o que não depende de framework: `Identity`, `Session` (com a regra de corte, `isEndedBy`) e `PublicPaths` (a lista de rotas públicas). `AuthenticateService` junta as regras: rota pública passa, sem `Bearer` ou token inválido é negado, token válido é conferido contra o corte de sessão. Se o Redis falhar, a requisição passa (política que já existia, agora num lugar só, com o log).

**Os filtros viram adaptadores de entrada finos.** `AuthenticationFilter` e `RateLimitFilter` só traduzem: tiram os cabeçalhos de identidade do cliente, chamam o caso de uso, assinam e escrevem os cabeçalhos do que foi concedido, ou respondem 401/429. A assinatura continua em `lexo-signing`, o contrato com os outros serviços.

**Os detalhes saem para adaptadores de saída.** `JjwtTokenVerifier` (jjwt), `RedisSessionCutoffAdapter` e `RedisRateLimitAdapter` (Redis). A aplicação não importa nada de jjwt, Spring Cloud Gateway, Spring Web nem de `lexo-signing`.

**As portas ficam reativas.** `Mono` aparece nas portas de entrada e de saída, porque o gateway roda sobre Netty e uma porta bloqueante travaria a thread de evento. O ArchUnit proíbe `reactor` só no domínio.

**Segredos viram uma validação de configuração.** `SecretsValidator` (pacote `config`) junta a recusa de iniciar com o segredo do JWT ou a chave de assinatura padrão fora do perfil de desenvolvimento. Antes, uma estava num bean próprio e a outra no construtor do filtro.

**O comportamento foi fixado antes de mover o código.** `GatewayBehaviorTest`, `GatewayRateLimitBehaviorTest` e `GatewayRedisDownBehaviorTest` sobem o gateway de verdade com as rotas de produção, um Redis (Testcontainers, `redis:7-alpine`) e um serviço de destino simulado, e só usam HTTP. Os 21 testes passaram sem alteração contra o código antigo e o novo.

## Consequências

- `CheckRateLimitService` só repassa a chamada para a porta. Fica por manter a mesma forma nos dois fluxos e porque a escolha da chave (o IP) é do adaptador de entrada; se o limite ganhar regra (por usuário, por rota), ela entra ali.
- Um token assinado, mas sem a declaração de escritório ou de papel, é aceito e segue sem esses cabeçalhos. Preservado de propósito e listado para as correções.
- O preflight de CORS é respondido antes do filtro de autenticação, pelo `CorsWebFilter`. O `WebTestClient` rejeita essas requisições por causa do endereço da requisição; o teste usa o cliente HTTP do JDK.

## Referências

- ADR 0002, layout de pacotes e regras de dependência.
- ADR 0005, sessão no caso de uso e erros de autenticação.
- ADR 0006, corte de sessão para tokens de acesso.
