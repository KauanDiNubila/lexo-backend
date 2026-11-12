# ADR 0004: Agendador, janela única e risco no domínio

- Status: Proposto
- Data: 2026-11-12

## Contexto

O processo-service foi o segundo serviço migrado e trouxe três casos que os serviços anteriores não tinham: um job agendado (o lembrete de prazos), uma trava de execução compartilhada entre instâncias e uma regra de negócio que dependia do relógio (o risco de um prazo). O código antigo resolvia os três dentro de uma classe de serviço, com `@Scheduled`, Redis e `Instant.now()` misturados à regra.

## Decisão

**O agendador é um adaptador de entrada.** `@Scheduled` fica em `adapter/in/schedule` e só chama o caso de uso. A mesma regra é acionada pela rota interna `/internal/cron/notify-deadlines`, então o caso de uso não sabe quem o chamou.

**"Uma vez por janela" é uma porta de saída.** `OncePerWindowPort.tryClaim(chave, janela)` reivindica uma janela de tempo e nunca a libera: quando a janela expira, a próxima execução passa. Isso impede que duas instâncias, ou uma chamada manual logo depois do cron, reenviem os mesmos lembretes. O nome evita `lock`, porque não existe `release` e a intenção precisa estar no nome, já que o código não leva comentários. O adaptador usa Redis (`SET NX` com TTL).

**O risco do prazo vive no domínio e recebe o horário.** `Deadline.riskAt(agora)` calcula o nível de risco a partir da data, do tipo (fator de risco no enum `DeadlineType`) e do estado. Quem chama passa o instante, vindo de um `Clock` injetado nos serviços, então os testes usam data fixa.

**Vocabulário.** `Andamento` vira `CaseMovement` (tradução usual de "andamento processual"). `Prazo` vira `Deadline`. O DTO do portal `PortalUpdate` mantém o nome, porque é contrato entre serviços.

## Consequências

- Os próximos jobs agendados repetem o formato: adaptador de entrada fino, caso de uso, porta de janela.
- O lembrete ainda faz chamadas remotas (usuário e fila de e-mail) dentro da transação do caso de uso. Isso já era assim e é dívida da Onda 5 (outbox).
- Regras que dependem de tempo no domínio recebem o instante por parâmetro, nunca chamam o relógio.

## Referências

- ADR 0002 (camadas e regras de dependência) e ADR 0003 (atalhos do piloto).
