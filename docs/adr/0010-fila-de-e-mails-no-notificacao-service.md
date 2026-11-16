# ADR 0010: Fila de e-mails no notificacao-service

- Status: Proposto
- Data: 2026-11-16

## Contexto

O notificacao-service lê a fila `lexo.email` do RabbitMQ e envia cada tarefa como e-mail de texto simples. Tinha duas classes: `ConsumidorEmail`, que recebia a mensagem, e `EmailService`, que montava o `SimpleMailMessage` e chamava o `JavaMailSender`. O serviço não tinha nenhum teste.

A política de falha não está no código. Está em `application.properties`: três tentativas com um segundo de intervalo e `default-requeue-rejected=false`, de modo que uma tarefa que continua falhando vai para a fila de mensagens mortas `lexo.email.dlq`, declarada no `lexo-messaging`.

## Decisão

**O envio vira um caso de uso e o SMTP vira um adaptador.** `SendEmailUseCase` recebe um `SendEmailCommand` e `SendEmailService` entrega um `Email` (destinatário, assunto, corpo) à porta de saída `MailPort`. `SmtpMailAdapter` implementa a porta com o `JavaMailSender` e é o único que conhece o remetente (`lexo.email.from`) e o `SimpleMailMessage`.

**A mensagem do RabbitMQ fica no adaptador de entrada.** `EmailQueueListener` é o único que conhece `TarefaEmail` e `RabbitConfig`. A aplicação e o domínio não importam nada de `org.springframework.amqp`, `org.springframework.mail` nem de `app.lexo.common.email`; o ArchUnit garante.

**Falha sobe até o broker.** Nem o caso de uso nem o listener capturam exceção. A repetição e a fila de mensagens mortas continuam sendo política do broker e do Spring AMQP, configurada em `application.properties`, como antes.

**O comportamento foi fixado antes de mover o código.** `EmailQueueBehaviorTest` usa um RabbitMQ de verdade (Testcontainers, `rabbitmq:3-alpine`, a imagem da compose) e um `JavaMailSender` simulado. Os quatro testes passaram sem alteração contra o código antigo e contra o novo: a tarefa vira um e-mail do remetente configurado; três tentativas e depois a fila de mortas; mensagem que não é uma tarefa vai direto para a fila de mortas; uma falha não trava as tarefas seguintes.

## Consequências

- `TarefaEmail`, `EnfileiradorEmail` e `RabbitConfig` continuam em português dentro do `lexo-messaging`, que os outros serviços também usam. Renomear é trabalho da limpeza das bibliotecas, não deste serviço.
- A fila `lexo.email.dlq` não tem leitor nem monitoramento, como a `lexo.eventos-dlt` do Kafka (ADR 0009).
- O serviço só envia texto simples. Nada valida o destinatário antes de tentar enviar, então um endereço vazio gasta as três tentativas antes de ir para a fila de mortas. Preservado de propósito e listado para as correções.

## Referências

- ADR 0002, layout de pacotes e regras de dependência.
- ADR 0008, falha do consumidor de auditoria.
- ADR 0009, mensagem inválida no tópico de eventos.
