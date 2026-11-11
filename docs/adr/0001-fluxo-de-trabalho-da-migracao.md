# ADR 0001: Fluxo de trabalho da migração hexagonal

- Status: Proposto
- Data: 2026-11-11

## Contexto

O Lexo tem 7 serviços de domínio, um gateway e um servidor de descoberta, e a migração para arquitetura hexagonal vai levar semanas. O branch `main` é o que aparece no portfólio e o que o Compose de produção sobe, então não pode ficar com serviços migrados pela metade. Misturar mudança de estrutura com correção de comportamento no mesmo PR impede saber o que quebrou.

## Decisão

- **Dois branches fixos.** `main` é o estável. `development` é a área de integração da migração.
- **Um branch e um PR por serviço**, saindo de `development` e voltando para ele: `refactor/hexagonal-cliente-service`, `refactor/hexagonal-financeiro-service` e assim por diante.
- **`development` vai para `main` por onda**, com o CI verde e a stack subindo no Compose, nunca serviço por serviço no meio de uma onda.
- **Correção urgente** sai direto de `main`. Em seguida `main` é trazida para `development` por merge, antes do próximo PR de serviço.
- **Dentro do PR, commits pequenos por etapa**, cada um compilando e com os testes verdes:
  1. testes que faltam para fixar o comportamento atual;
  2. domínio e portas;
  3. casos de uso e adaptadores de saída;
  4. adaptadores de entrada e troca das chamadas antigas;
  5. remoção do código antigo e entrada do serviço no `ArquiteturaTest`.
- **Merge sem squash** nos PRs de serviço, para as etapas ficarem no histórico.
- **Nenhum PR de migração muda rota, JSON, código de erro ou tabela.** Correção de comportamento vai em PR separado para `main`, com teste.
- **Ponto de partida** marcado pela tag `pre-hexagonal`. O acompanhamento fica na issue #23.
- **Revisão thermo-nuclear** nos PRs que mudam estrutura (migração de serviço, trilha distribuída e limpeza final), rodada pelo autor antes de abrir o PR. Não se aplica a PRs só de teste, CI, ADR ou dependências.
- **Decisão nova** durante a migração vira ADR no mesmo PR em que nasce.

## Consequências

- `main` sempre pode ir para produção durante a migração.
- Cada PR é revisável sozinho e pode ser revertido sem afetar os outros serviços.
- Há dois merges por onda, e `development` pode se afastar de `main`. O merge de `main` para `development` depois de cada correção urgente evita isso.
- Durante a migração, serviços migrados e não migrados convivem.

## Referências

- Hombergs, cap. 12, "A experiência é rainha": começar por um módulo pequeno e ajustar o estilo com o que se aprende.
