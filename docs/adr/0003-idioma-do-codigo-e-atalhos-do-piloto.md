# ADR 0003: Idioma do código e atalhos decididos no piloto

- Status: Proposto
- Data: 2026-11-11

## Contexto

O cliente-service foi o primeiro serviço migrado para a arquitetura da ADR 0002. A migração exigiu decisões que valem para os outros serviços e que a ADR 0002 deixou em aberto: o idioma do código, onde se pode pular a porta de entrada, como lidar com exceções sem tipos de web e como não quebrar a leitura de dados antigos.

## Decisão

**Idioma do código.** Todo o código está em inglês: classes, métodos, variáveis, pacotes, constantes, nomes de teste e o texto dos `@DisplayName`. A documentação (ADRs, README) continua em português. Serviços que ainda não foram migrados mantêm os nomes atuais até a vez deles.

**O que não muda, porque é contrato.** Rotas (`/api/clientes`), nomes de campo de JSON, códigos de erro (`NAO_ENCONTRADO`), mensagens de erro mostradas ao usuário, valores gravados no banco (`ATIVO`, `PENDENTE`), tipos de evento (`CLIENTE_CRIADO`) e nomes de serviço no Eureka. Quando um campo de JSON tem nome em português, o record de resposta usa nome Java em inglês com `@JsonProperty("nomeAntigo")`. Isso é um atalho: a anotação de JSON fica nos records da aplicação em vez de existir um mapper no adaptador web. O critério para sair: a API mudar de versão, ou um segundo formato de resposta aparecer.

**Vocabulário fixo.** Escritório vira `Organization` (combina com a coluna `organization_id`), CPF ou CNPJ vira `TaxId`, processo vira `case` e honorário vira `fee`.

**Atalho: leituras sem regra.** Buscar, listar, opções, resumo e existência são portas de entrada de consulta, cada uma uma interface, implementadas por uma única classe (`ClientQueryService`) em vez de uma classe por leitura. O critério para sair do atalho: uma leitura ganha regra de negócio própria, ou precisa de um segundo adaptador de entrada diferente. Aí ela vira o seu próprio serviço.

**Exceções sem web.** A aplicação e o domínio lançam `ResourceNotFoundException` e `InvalidDataException`, definidas em `lexo-common` sem dependência de Spring. O `GlobalExceptionHandler` as traduz para os mesmos status e códigos que `ApiException` devolve (404 `NAO_ENCONTRADO` e 400 `REQUISICAO_INVALIDA`). `ApiException` continua existindo para os serviços ainda não migrados.

**Validação de entrada e dado salvo.** Formato e obrigatoriedade são validados na entrada: em `TaxId.of`, nos comandos e na requisição web. O que já está no banco é reconstituído sem validar de novo (`TaxId.stored`), porque um registro antigo fora da regra de hoje não pode deixar de ser lido.

**Contratos entre serviços.** O portal usa direto os DTOs `ProcessoPortal` e `HonorarioPortal` de `lexo-messaging`, sem mapper. A aplicação também usa a `Pagina` de `lexo-common`, que é um record sem dependência de Spring nas partes que a aplicação usa. Esses nomes de biblioteca ainda estão em português e mudam junto com a biblioteca.

**Cache.** O cache das opções fica no adaptador de persistência e é invalidado quando um cliente é salvo ou excluído. O nome do cache ganhou sufixo (`clientes-opcoes-v2`) para o primeiro deploy não ler objetos serializados da classe antiga.

## Consequências

- Os próximos serviços repetem estas escolhas sem rediscutir, a menos que o caso peça outra.
- O código novo nunca mistura idiomas. Os poucos nomes em português que sobram são contrato externo e ficam sob `@JsonProperty` ou em strings.
- Os atalhos em vigor são dois (leituras agrupadas e `@JsonProperty` na aplicação), com saída escrita acima e revisada a cada serviço.
- Enquanto houver serviços não migrados, `lexo-common` mantém `ApiException` e as exceções puras.

## Referências

- Hombergs, cap. 11, atalhos conscientes (compartilhar modelos entre casos de uso); cap. 4, validação da entrada; cap. 8, mapeamento.
