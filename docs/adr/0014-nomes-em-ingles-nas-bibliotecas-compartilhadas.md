# ADR 0014: Nomes em inglês nas bibliotecas compartilhadas

- Status: Proposto
- Data: 2026-11-17

## Contexto

A ADR 0003 decidiu que o código é em inglês e que o contrato externo (rotas, JSON, códigos de erro, banco) fica como está. Os serviços migraram com isso, mas três bibliotecas ficaram para o fim, porque mudar um nome ali atinge todos os serviços ao mesmo tempo:

- `lexo-signing`: `GatewaySignature` tinha `H_ASSINATURA`, `CHAVE_DEV`, `exigirChaveSegura`, `dentroDaJanela` e parâmetros em português.
- `lexo-messaging`: a tarefa de e-mail era `TarefaEmail` (campos `tipo`, `destinatario`, `assunto`, `corpo`) e a classe que a coloca na fila era `EnfileiradorEmail.enfileirar`.
- `lexo-common`: `HeaderAuthenticationFilter` e `InternalKeyFilter` tinham campos e métodos em português, e `BaseEntity` estava num pacote chamado `domain`, embora seja uma classe de persistência (JPA) que só os adaptadores de saída estendem.

## Decisão

**Só nomes de Java mudam. O que trafega continua igual.**

- `GatewaySignature`: `H_SIGNATURE`, `DEV_KEY`, `requireSecureKey`, `withinWindow`. Os valores dos cabeçalhos (`X-Gateway-Ts`, `X-Gateway-Sig`), o formato do conteúdo assinado e a assinatura de um conteúdo conhecido ficam fixados por `WireContractTest` em `lexo-signing`, escrito antes dos renomes.
- `EmailTask(type, recipient, subject, body)` e `EmailPublisher.enqueue`. O JSON da fila continua `tipo`, `destinatario`, `assunto` e `corpo`, por `@JsonProperty`. Uma mensagem que já estava na fila, com a classe antiga no cabeçalho `__TypeId__`, ainda é lida como `EmailTask`, porque o Spring AMQP prefere o tipo do parâmetro do listener; há um teste para isso.
- `BaseEntity` passa para `app.lexo.common.persistence`. O pacote `domain` da biblioteca deixa de existir.
- Os testes das bibliotecas passam para inglês.

**Ficam em português de propósito**, por serem contrato com quem chama ou com quem lê: os códigos de erro (`NAO_ENCONTRADO`, `REQUISICAO_INVALIDA`, `SERVICO_INDISPONIVEL`, `CONFLITO`), as mensagens de erro, os nomes de campos JSON da fila de e-mail, o prefixo `/internal/`, o texto das exceções de configuração (`GATEWAY_SIGNING_KEY ausente...`) e o nome do principal `servico-interno`.

**Uma exceção que não é contrato:** as constantes de `ErrorCode` têm o nome Java igual ao valor (`NAO_ENCONTRADO = "NAO_ENCONTRADO"`). O valor é contrato; o nome da constante não é, mas renomeá-las exigiria separar nome e valor em todos os serviços, então ficam como estão. O método, que era `paraStatus`, passou a `forStatus`.

## Consequências

- Nenhuma mudança de comportamento: a suíte inteira passa sem alteração de asserção, só com os nomes novos.
- Uma mensagem que já estava na fila antes do deploy, com a classe antiga no cabeçalho, é lida pelo consumidor novo (há um teste). O sentido contrário, uma mensagem nova lida por um consumidor ainda na versão antiga, não foi testado: ele deve funcionar pela mesma regra (o tipo vem do parâmetro do listener, não do cabeçalho), mas é dedução. Para evitar a dúvida, o consumidor (notificacao) pode ser atualizado antes dos produtores (auth e processo).
- Quem escrever um serviço novo encontra a biblioteca em inglês. O que não está em inglês é contrato, com a exceção dos nomes das constantes de `ErrorCode`, descrita acima.

## Referências

- ADR 0003, idioma do código e atalhos do piloto.
