# ADR 0005: Sessão no caso de uso e erros de autenticação

- Status: Proposto
- Data: 2026-11-12

## Contexto

O auth-service foi o quarto serviço migrado e o primeiro com segurança no centro: login, renovação de sessão, 2FA e equipe. Ele trouxe quatro situações que as ADRs 0002 a 0004 não cobriam: quem emite o cookie de renovação, como erros de autenticação com código próprio saem da aplicação sem tipos de web, o que a aplicação pode importar de `lexo-common.security`, e de onde vêm os tipos de evento do auth.

## Decisão

**A sessão é montada no caso de uso.** Cadastro, login e renovação devolvem um `SessionResponse` com o token de acesso, os dados do usuário e o token de renovação em texto. O adaptador web grava o cookie e responde só com `accessToken` e `user`. O token de renovação nunca entra no corpo da resposta. Antes, o controller chamava dois serviços em transações separadas. Agora emitir o token de renovação faz parte da mesma transação que autenticou, então uma falha ao emitir desfaz o cadastro ou o login em vez de deixar o usuário sem sessão.

**Renovação mantém a revogação mesmo quando falha.** O caso de uso de renovação usa `noRollbackFor` nas duas exceções de token. Reapresentar um token já revogado revoga todas as sessões do usuário e depois lança o erro. Sem essa regra, o erro desfaria a revogação, que é exatamente o que a detecção de roubo precisa preservar. Um teste de integração fixa isso.

**Erros de autenticação são exceções de domínio sem código HTTP.** Cada situação com código próprio (`CREDENCIAIS_INVALIDAS`, `TOTP_REQUIRED`, `TOTP_INVALID`, `REFRESH_INVALIDO`, `REFRESH_EXPIRADO`, `EMAIL_EM_USO`, `SENHAS_NAO_COINCIDEM`) é uma classe pequena do domínio com a mensagem mostrada ao usuário. Um `@RestControllerAdvice` do adaptador web, com `@Order(HIGHEST_PRECEDENCE)`, traduz cada uma para status e código. A ordem importa: o `GlobalExceptionHandler` de `lexo-common` captura `Exception`, então um tratador sem precedência seria ignorado. Os erros genéricos (404, 400, 409 sem código próprio, 503) continuam usando as exceções de `lexo-common`.

**A aplicação pode usar `Role`, não `AuthUser`.** `Role` é um enum puro e faz parte da linguagem do negócio (quem pode convidar quem). A regra de arquitetura que proibia `app.lexo.common.security` na aplicação passou a proibir só `AuthUser`, o objeto que carrega o contexto de segurança do Spring. O adaptador web converte `AuthUser` em `Actor` e ids de domínio.

**Eventos do auth entram no `EventType`.** As seis ações (`CONVIDOU_USUARIO`, `REVOGOU_CONVITE`, `ALTEROU_PAPEL`, `REMOVEU_USUARIO`, `ATIVOU_2FA`, `DESATIVOU_2FA`) viraram valores do enum, com o código gravado no Kafka como valor, como nos outros serviços. O teste de contrato em `lexo-messaging` fixa a lista.

**Tempo e aleatoriedade.** Todo serviço recebe um `Clock`. O token de acesso, a validade do convite e a do token de renovação usam esse relógio. A geração do token de renovação e o hash SHA-256 são Java puro e ficam no domínio (`RefreshTokenSecret`), porque não dependem de framework. A cifra do segredo TOTP, o TOTP em si, o BCrypt e o contador no Redis ficam atrás de portas.

## Consequências

- Os próximos serviços com erro de código próprio seguem o mesmo molde: exceção de domínio mais tradutor no adaptador, sempre com precedência.
- Quem ler o `AuthController` vê só HTTP: leitura do cookie, endereço do cliente e escrita do cookie.
- O `SessionIssuer` lê `lexo.refresh-expiration-days` com `@Value`. É o único ponto da aplicação que conhece uma propriedade. O critério para sair do atalho: aparecer uma segunda configuração de sessão, que pede um objeto de configuração próprio.
- O ator de um evento sem nome nem e-mail no cabeçalho passa a ser "Usuário". Antes variava (`Admin` na equipe, vazio no 2FA). O gateway sempre envia o nome, então na prática nada muda.

## Referências

- Hombergs, cap. 4, validação da entrada e casos de uso; cap. 8, mapeamento; cap. 9, montagem.
- ADR 0002 (camadas e regras de dependência) e ADR 0003 (exceções sem web, `EventType`).
