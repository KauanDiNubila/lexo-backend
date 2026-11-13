# ADR 0007: Limite de uso e geração de texto no ia-service

- Status: Proposto
- Data: 2026-11-13

## Contexto

O ia-service não tem banco e faz duas coisas além de montar prompts: chama o Gemini e limita quantas chamadas cada escritório pode fazer por minuto. Antes da migração o controller chamava o limitador e depois o serviço, então a regra "toda chamada de IA gasta uma unidade do limite" dependia de cada endpoint lembrar de fazer isso. O serviço de IA misturava num só lugar os três casos de uso, a escolha entre resposta do modelo e texto automático, e os prompts.

## Decisão

**Três casos de uso, um por funcionalidade.** Resumir processo, conversar e rascunhar petição são portas de entrada separadas (`SummarizeCaseUseCase`, `ChatUseCase`, `DraftPetitionUseCase`). Cada serviço monta o próprio prompt e o próprio texto automático. O que é comum fica em `CaseText` (as quatro linhas do processo e o valor padrão de campo ausente).

**O limite é gasto dentro do caso de uso.** `UsageQuota` é chamado na primeira linha de cada serviço. O controller não sabe que existe limite. A política (máximo por minuto e janela de um minuto) fica na aplicação (`AiSettings` e a janela em `UsageQuota`); o contador fica no adaptador do Redis (`RedisRateLimitAdapter`), que só devolve se a chamada cabe. Passar do limite lança `RateLimitExceededException`, do domínio, e o `IaExceptionHandler` local a traduz para 429 `REQUISICOES_EM_EXCESSO`.

**Falha do Redis recusa a chamada.** O adaptador lança `ServiceUnavailableException` (503 `SERVICO_INDISPONIVEL`) e o Gemini não é chamado. É o comportamento que já existia e segue a mesma lógica do limite de login do auth: sem contador confiável, não se gasta dinheiro com o modelo.

**O Gemini fica atrás de uma porta de saída.** `TextGenerationPort` tem dois métodos: `generate(prompt)` e `converse(instrução, mensagens)`. Devolvem `Optional<String>`: vazio quando não há chave, quando a chamada falha ou quando a resposta vem sem texto. Os casos de uso tratam vazio como "usar o texto automático" e marcam a origem como `mock`. A tradução do papel `ASSISTANT` para `model` e o formato do corpo do Gemini ficam no adaptador.

**A origem do texto é do domínio.** `Source.MODEL` vale `gemini` e `Source.FALLBACK` vale `mock`, os dois valores que a API devolve no campo `fonte`.

**Mensagem em branco não chega ao domínio.** `ChatMessage` exige papel e texto não vazio. A conversa que o cliente envia pode ter mensagens em branco; o adaptador web as descarta antes de montar o comando, como o serviço antigo fazia.

**O que não muda.** Rotas (`/api/ia/...`), nomes de campo de JSON (`mensagens`, `papel`, `texto`, `contexto`, `pedido`, `prazos`, `resumo`, `resposta`, `fonte`), mensagens de erro, textos dos prompts e dos modelos automáticos, e as propriedades `lexo.ia.*`.

## Consequências

- Um endpoint novo de IA só é protegido pelo limite se o caso de uso chamar `UsageQuota`. Os testes de cada serviço fixam que a primeira ação é gastar o limite.
- Trocar de provedor de modelo é trocar um adaptador. O valor `gemini` do campo `fonte` é contrato e continua assim.
- A chave do Gemini ainda vai na query string da URL e a mensagem de erro do cliente HTTP pode incluí-la no log. Isso foi preservado de propósito nesta migração e é o primeiro item das correções.

## Referências

- ADR 0003, idioma do código e atalhos do piloto.
- ADR 0005, exceções de domínio traduzidas por um handler local.
