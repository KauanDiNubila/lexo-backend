# ADR 0011: Repetição do envio de e-mail

- Status: Proposto
- Data: 2026-11-16

## Contexto

O notificacao-service tentava enviar cada tarefa 3 vezes com 1 segundo entre as tentativas e depois mandava a tarefa para `lexo.email.dlq`. Uma queda do SMTP de um minuto bastava para mandar todos os e-mails desse minuto (convites, lembretes de prazo) para uma fila que ninguém lê. A perda era silenciosa.

Foi verificado que não há outro problema evidente no envio: destinatário nulo, vazio ou com dois endereços falha no `JavaMailSender` antes de abrir conexão, e um assunto com quebra de linha é dobrado pelo Jakarta Mail como continuação do `Subject`, sem criar cabeçalho novo.

## Decisão

**Seis tentativas com espera crescente.** `max-attempts=6`, `initial-interval=2000`, `multiplier=2`, `max-interval=30000`: as esperas são 2, 4, 8, 16 e 30 segundos, ao todo cerca de um minuto antes de a tarefa ir para a fila de mensagens mortas. A política continua nas propriedades do Spring AMQP; nenhum código novo.

**O teste de comportamento encurta as esperas.** `EmailQueueBehaviorTest` sobrescreve o intervalo para 50 ms sem multiplicador e confirma as 6 tentativas. `RetryConfigurationTest` lê o `application.properties` de produção e calcula a espera total, que precisa ficar entre 45 e 90 segundos.

## Consequências

- Uma queda de SMTP de até um minuto deixa de perder e-mails. Uma queda maior ainda manda tudo para `lexo.email.dlq`.
- Enquanto uma tarefa espera, o consumidor fica parado nela (a repetição roda na thread do listener), então as tarefas seguintes atrasam até um minuto. Com um consumidor só e o volume atual do Lexo isso é aceitável. Se o volume crescer, o próximo passo é repetição por filas de espera (retry com TTL) em vez de bloquear a thread.
- A fila de mortas continua sem leitor nem monitoramento. É o conserto de verdade para a perda silenciosa e fica fora deste PR.

## Referências

- ADR 0010, fila de e-mails no notificacao-service.
