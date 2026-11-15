# ADR 0009: Mensagem inválida no tópico de eventos

- Status: Proposto
- Data: 2026-11-14

## Contexto

Auditoria, financeiro e processo leem o tópico `lexo.eventos` com um `JsonDeserializer` direto. Quando uma mensagem do tópico não é um JSON de `DomainEvent`, a falha acontece dentro do cliente Kafka, antes de o Spring entregar o registro ao listener. O tratador de erros (`DefaultErrorHandler`) não consegue pular o registro, então a leitura volta ao mesmo offset a cada poll. O consumidor fica parado nessa mensagem para sempre e grava o erro no log em laço, dezenas de milhares de linhas por segundo. Foi descoberto ao escrever um teste com um Kafka de verdade para o auditoria.

O `lexo-common` já tinha repetição e fila de mensagens mortas (`KafkaErrorHandlingConfig`), mas só para falhas dentro do listener. Faltavam duas coisas: o consumidor não transformava a falha de leitura em uma exceção que o tratador enxerga, e a fila de mortas só sabia publicar objetos, não os bytes originais de uma mensagem que nunca virou objeto.

## Decisão

**A leitura de JSON fica dentro de um `ErrorHandlingDeserializer`.** Os três consumidores configuram `spring.kafka.consumer.value-deserializer=ErrorHandlingDeserializer` e `spring.deserializer.value.delegate.class=JsonDeserializer`. Quando o JSON é inválido, o registro chega ao tratador com uma `DeserializationException`, que o `DefaultErrorHandler` trata como definitiva: sem as repetições, direto para a fila de mortas, e o consumo segue com a próxima mensagem.

**A fila de mortas publica os bytes originais.** `KafkaErrorHandlingConfig` monta dois modelos de envio sobre a mesma fábrica de produtores: um com `ByteArraySerializer` para o valor, usado quando o valor é `byte[]`, e o comum, para objetos. A mensagem inválida vai para `lexo.eventos-dlt` exatamente como chegou, sem ser reescrita como JSON de uma string em base64.

**Quem publica na fila de mortas precisa de produtor configurado.** O financeiro não tinha `spring.kafka.producer.*`; ganhou `StringSerializer` para a chave e `JsonSerializer` para o valor, como cliente, processo e auditoria.

## Consequências

- Uma mensagem inválida deixa de parar o consumo e passa a ficar na fila `lexo.eventos-dlt`, que ninguém lê ainda. Ela precisa de monitoramento, mas não de um consumidor que regrave os eventos.
- A configuração está repetida nos três serviços. Cada um tem um teste que lê o `application.properties` de produção e falha se o `ErrorHandlingDeserializer` ou o produtor JSON saírem.
- O comportamento de ponta a ponta (mensagem inválida vai para a fila de mortas e o consumo continua) é provado no auditoria com um broker real, porque a lógica fica no `lexo-common`, que os três usam.

## Referências

- ADR 0008, falha do consumidor de auditoria.
