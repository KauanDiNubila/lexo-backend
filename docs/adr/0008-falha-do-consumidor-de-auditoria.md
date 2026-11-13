# ADR 0008: Onde fica a política de falha do consumidor de auditoria

- Status: Proposto
- Data: 2026-11-13

## Contexto

O auditoria-service lê eventos do tópico `lexo.eventos` e grava um registro por evento. O serviço antigo engolia qualquer falha dentro de `AuditoriaService.registrar`: se o banco recusasse o registro, o erro ia para o log e o consumidor seguia. A decisão de "não derrubar o consumidor" estava misturada com a gravação. Ao mover a gravação para um caso de uso transacional, essa mistura deixa de funcionar: capturar uma exceção dentro de uma transação que já está marcada para reversão faz o commit falhar com `UnexpectedRollbackException`.

## Decisão

**O caso de uso deixa a falha subir.** `RegisterAuditEntryService` é transacional, grava e não captura nada. Uma entrada que viola uma regra (sem escritório, sem nome de usuário, sem ação, sem descrição) é recusada pelo `AuditEntry` com `IllegalArgumentException`, antes de chegar ao banco.

**A política de falha fica no adaptador de entrada.** `AuditEventListener` captura qualquer exceção do caso de uso, grava o erro no log com a ação e o escritório, e devolve normalmente. O comportamento observável é o de antes: o evento é descartado e o consumidor continua. Se a política mudar (tentar de novo, mandar para uma fila de mensagens mortas), muda num lugar só, sem tocar na regra de gravação.

**Registros imutáveis.** `AuditEntry` não tem operação de atualização e o adaptador de persistência só insere. Um id e uma data só existem depois de gravado.

**Leitura.** `AuditEntryQueryService` pede as 200 entradas mais recentes do escritório; o limite é política da aplicação e a ordenação é do adaptador de persistência.

## Consequências

- Um evento que não pode ser gravado se perde sem aviso além do log. Isso foi preservado de propósito e é o primeiro item das correções: o consumo com falha deveria ser repetido e, depois de algumas tentativas, ir para uma fila de mensagens mortas.
- O instante do registro é o da gravação, não o do evento (`ocorridoEm`). Também preservado e listado nas correções.
- O teste do listener usa o método `@KafkaListener` descoberto por reflexão, então os mesmos testes de comportamento valem antes e depois de renomear a classe.

## Referências

- ADR 0002, layout de pacotes e regras de dependência.
- ADR 0003, idioma do código e atalhos do piloto.
