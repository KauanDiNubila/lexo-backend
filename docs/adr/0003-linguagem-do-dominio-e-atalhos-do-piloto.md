# ADR 0003: Linguagem do domínio e atalhos decididos no piloto

- Status: Proposto
- Data: 2026-11-11

## Contexto

O cliente-service foi o primeiro serviço migrado para a arquitetura da ADR 0002. A migração exigiu decisões que valem para os outros serviços e que a ADR 0002 deixou em aberto: a língua do domínio, onde se pode pular a porta de entrada, como lidar com exceções sem tipos de web e como não quebrar leitura de dados antigos.

## Decisão

**Língua do domínio.** O modelo, as portas, os comandos e os casos de uso usam português do negócio: `Cliente`, `Processo`, `Prazo`, `Honorario`. Os nomes de tabela, de coluna, de rota e de campo de JSON não mudam. Por isso a entidade JPA (`ClienteJpaEntity`) mantém os nomes de campo de hoje (`name`, `document`), e as respostas mantêm os mesmos nomes de componente (`ClienteResponse.name`). Isso preserva também a ordenação por `?sort=name`.

**Atalho: leituras sem regra.** Buscar, listar, opções, resumo e existência são portas de entrada de consulta, cada uma uma interface, implementadas por uma única classe de consulta (`ClienteQueryService`) em vez de uma classe por leitura. O critério para sair do atalho: uma leitura ganha regra de negócio própria, ou precisa de um segundo adaptador de entrada diferente. Aí ela vira o seu próprio serviço.

**Exceções sem web.** A aplicação e o domínio lançam `RecursoNaoEncontradoException` e `DadoInvalidoException`, definidas em `lexo-common` sem dependência de Spring. O `GlobalExceptionHandler` as traduz para os mesmos status e códigos que `ApiException` devolve (404 `NAO_ENCONTRADO` e 400 `REQUISICAO_INVALIDA`). `ApiException` continua existindo para os serviços ainda não migrados.

**Validação de entrada e dado salvo.** Formato e obrigatoriedade são validados na entrada: no `CpfCnpj.de`, nos comandos e na requisição web. O que já está no banco é reconstituído sem validar de novo (`CpfCnpj.armazenado`), porque um registro antigo fora da regra de hoje não pode deixar de ser lido.

**Contratos entre serviços.** O portal usa direto os DTOs `ProcessoPortal` e `HonorarioPortal` de `lexo-messaging`, sem mapper. A aplicação também usa a `Pagina` de `lexo-common`, que é um record sem dependência de Spring nas partes que a aplicação usa.

**Cache.** O cache das opções fica no adaptador de persistência e é invalidado quando um cliente é salvo ou excluído. O nome do cache ganhou sufixo (`clientes-opcoes-v2`) para o primeiro deploy não ler objetos serializados da classe antiga.

## Consequências

- Os próximos serviços repetem estas escolhas sem rediscutir, a menos que o caso peça outra.
- O atalho das leituras é o único em vigor. Pular porta de entrada em CRUD sem regra só é aceito com a mesma contrapartida: a saída é escrita aqui e revisada a cada serviço.
- Enquanto houver serviços não migrados, `lexo-common` mantém `ApiException` e as exceções puras.

## Referências

- Hombergs, cap. 11, atalhos conscientes (pular as portas de entrada e compartilhar modelos); cap. 4, validação da entrada; cap. 8, mapeamento.
