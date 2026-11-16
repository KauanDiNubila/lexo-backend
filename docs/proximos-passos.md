# Próximos passos

Estado em 17/11/2026, depois da migração de todos os serviços para a arquitetura hexagonal (ADR 0001 a 0014). Este arquivo lista o que ficou para trás, em erro ou em melhoria, para quem for continuar o projeto. Cada item diz o que é, por que importa e o tamanho aproximado do trabalho: **P** (pequeno, um PR curto), **M** (médio, alguns dias de PRs) ou **G** (grande, uma frente própria).

Prioridades: **1** antes ou logo depois do próximo deploy, **2** próximas semanas, **3** quando fizer sentido.

---

## 1. Testes e qualidade

### Como está hoje

| Módulo | Testes | Linhas cobertas | Ramos cobertos | Observação |
| --- | --- | --- | --- | --- |
| api-gateway | 84 | 98,6% | 96,1% | só HTTP contra gateway real, Redis real |
| auth-service | 222 | 97,7% | 89,7% | |
| cliente-service | 85 | 97,3% | 82,6% | |
| processo-service | 116 | 97,3% | 80,9% | |
| financeiro-service | 76 | 97,7% | 80,8% | |
| auditoria-service | 45 | 98,1% | 87,5% | inclui Kafka real |
| notificacao-service | 18 | 94,3% | 50,0% | inclui RabbitMQ real |
| ia-service | 83 | 99,1% | 94,3% | |
| lexo-signing | 12 | 91,3% | 94,4% | |
| lexo-common | 19 | 50,5% | 61,5% | ver abaixo |
| lexo-messaging | 8 | 49,0% | 0% | ver abaixo |
| **Total** | **768** | | | `mvn verify` leva cerca de 4 minutos no CI |

A camada de aplicação está em 100% de linhas em todos os serviços e o domínio entre 92% e 100%. Cada serviço tem um teste de arquitetura (ArchUnit, de 6 a 9 regras) que impede o domínio de importar framework e os adaptadores de se enxergarem.

O que está bom: testes de comportamento escritos **antes** de cada migração e rodando sem alteração depois; bancos, Kafka, RabbitMQ e Redis reais via Testcontainers, sem H2; fakes em vez de mocks na camada de aplicação; testes de contrato dos formatos que trafegam; prova de mutação feita à mão em cada PR (um trecho é quebrado de propósito para ver se algum teste falha); nenhum `@Disabled`.

### O que falta

| # | Item | Por que importa | Tam. | Prio. |
| --- | --- | --- | --- | --- |
| T1 | **Teste de fumaça da stack no CI.** Hoje ele foi feito à mão (subir o Compose e passar por registrar, entrar, cliente, processo, auditoria, e-mail, portal, IA e 429). O roteiro não está no repositório. | Nenhum teste automático prova serviços diferentes conversando de verdade. As chamadas Feign entre serviços são simuladas nos testes de cada um. | M | 1 |
| T2 | **Porta de cobertura no CI.** O JaCoCo gera relatório, mas não há mínimo: a cobertura pode cair sem o CI avisar. | Sem porta, o número de hoje é só uma foto. | P | 1 |
| T3 | **Teste de mutação automático** (PIT) em domínio e aplicação. Os scripts atuais ficam fora do repositório. | Cobertura de linha diz que o código rodou, não que o teste o protege. A prova de mutação à mão já achou lacunas reais. | M | 2 |
| T4 | **Frontend sem nenhum teste automático.** O CI só faz `npm run build`. Não roda nem o `lint`. | Toda a interface depende de olho humano. | M | 2 |
| T5 | **Testes das bibliotecas.** `lexo-common` e `lexo-messaging` têm cobertura própria baixa: `PageParams`, `PageResult`, `PageParamsMapper`, `ApiException`, `EventPublisher`, `KafkaErrorHandlingConfig`, `BaseEntity`, `RabbitConfig` e `EmailPublisher` não têm teste dentro do módulo. | Esse código é exercitado pelos testes de integração dos serviços, mas o relatório de cada serviço não o conta, então a falta não aparece. | P | 2 |
| T6 | **Contrato entre serviços** (por exemplo, testes de contrato dos clientes Feign). | Se o `cliente-service` mudar uma resposta, o `processo-service` só descobre em produção, porque o teste dele usa um simulado. | M | 2 |
| T7 | **Dois `Thread.sleep` em testes**: `InvoiceApiBehaviorTest` (financeiro) e `CasePersistenceAdapterIntegrationTest` (processo). | Espera por tempo fixo é fonte clássica de teste instável. Trocar por relógio injetado ou espera por condição. | P | 3 |
| T8 | **Tempo do `verify`.** São 11 módulos em série. | Hoje são cerca de 4 minutos, aceitável; passa a doer se a suíte dobrar. Paralelizar módulos ou cachear imagens. | P | 3 |
| T9 | **Teste de carga e de resiliência** (Redis, Kafka ou RabbitMQ fora do ar durante tráfego). | O comportamento "fail-open" do Redis é decisão tomada, mas só foi testado por unidade. | G | 3 |

---

## 2. Segurança

| # | Item | Por que importa | Tam. | Prio. |
| --- | --- | --- | --- | --- |
| S1 | **Girar a chave do Gemini.** Ação manual do dono do projeto. | A rotação foi recomendada na revisão de segurança e ainda não foi feita. | P | 1 |
| S2 | **Conferir o `X-Forwarded-For` depois do deploy.** Mandar uma requisição com o cabeçalho forjado e ver que o limite de requisições não muda. | O Caddy de produção em si não foi testado, só uma imagem igual (ADR 0013). | P | 1 |
| S3 | **Dois alertas do Dependabot abertos** (um baixo, um médio). | Dependência com vulnerabilidade conhecida. Ver quais e atualizar. | P | 1 |
| S4 | **`/docs/` (Swagger) é público em produção.** | Mostra todas as rotas e formatos a quem não tem conta. É decisão de exposição: manter só em desenvolvimento ou proteger. | P | 2 |
| S5 | **Trocar a senha ou ligar o 2FA não encerra as sessões abertas.** O mecanismo de corte de sessão existe (ADR 0006) e só falta acioná-lo nesses dois casos. | Quem roubou um token continua com acesso depois de a vítima trocar a senha. | P | 2 |
| S6 | **O bloqueio de login por e-mail permite travar a conta de outra pessoa** errando a senha de propósito. | Negação de serviço dirigida. É decisão de design: bloquear por e-mail e endereço juntos, ou avisar o dono. | M | 3 |
| S7 | **Os cinco cabeçalhos de identidade** (`X-User-Id` etc.) estão escritos duas vezes: no gateway e no `lexo-common`. | Se um lado mudar, o outro quebra sem aviso. Mover os nomes para o `lexo-signing`, junto dos dois da assinatura. | P | 3 |

---

## 3. Confiabilidade e operação

| # | Item | Por que importa | Tam. | Prio. |
| --- | --- | --- | --- | --- |
| O1 | **As filas de mensagens mortas não têm leitor nem alerta**: `lexo.eventos-dlt` (Kafka) e `lexo.email.dlq` (RabbitMQ). | Uma mensagem que cai ali deixa de ser processada em silêncio. O conserto de verdade é monitorar e ter um jeito de reprocessar. | M | 1 |
| O2 | **O que fazer se a migração V3 do cliente der errado.** Ela é irreversível (os tokens do portal passam a ser guardados como hash). | Sem plano de volta, só o backup protege. Escrever o passo a passo no `docs/deploy.md`. | P | 1 |
| O3 | **`docs/deploy.md` desatualizado.** Falta a ordem desta rodada: notificacao antes de auth e processo, gateway e auth juntos, Caddy recriado, backup do banco do cliente antes da V3. | O deploy é manual e depende de a pessoa lembrar. | P | 1 |
| O4 | **Deploy manual.** O CI só testa; não publica nada. | Cada deploy repete passos à mão. Um workflow que constrói, sobe e roda o teste de fumaça (T1) fecharia o ciclo. | G | 3 |
| O5 | **A auditoria cresce sem limite.** A listagem devolve as 200 mais recentes, sem paginação nem filtro, e não há retenção. | A tabela só aumenta; a tela fica cega para registros antigos. | M | 2 |
| O6 | **Um evento entregue duas vezes gera dois registros** na auditoria. | Evitar exige um identificador no `DomainEvent` e checagem na gravação, o que muda o contrato de todos os serviços. | M | 3 |
| O7 | **A publicação do evento é "melhor esforço".** Se o Kafka estiver fora, `EventPublisher` registra o erro e segue; o evento se perde. | A auditoria fica com buracos. A solução é o padrão outbox (item A1). | G | 2 |
| O8 | **O envio de e-mail repete só por bloqueio de thread.** Durante os cerca de 60 segundos de espera o consumidor fica parado naquela tarefa (ADR 0011). Uma recusa permanente do SMTP (endereço sem arroba) também gasta as tentativas. | Se o volume crescer, as tarefas seguintes atrasam. O próximo passo é repetição por filas de espera com TTL. | M | 3 |
| O9 | **O limite de uso do ia-service é gasto mesmo sem chave do Gemini.** | O usuário perde cota por uma chamada que só devolve o resumo automático. | P | 3 |
| O10 | **Um elemento nulo na lista `prazos` ou `mensagens` do ia-service gera 500**, e `@Valid` não cobre. | Um corpo malformado derruba a requisição com erro do servidor em vez de 400. | P | 3 |
| O11 | **O log da auditoria avisa que não achou um provedor de Bean Validation** na subida. | Provavelmente inofensivo (só há rotas de leitura), mas não foi confirmado. Ou se adiciona o provedor, ou se confirma e se remove a dependência. | P | 3 |
| O12 | **O notificacao tenta conectar ao RabbitMQ antes de a porta abrir** e reconecta sozinho 5 segundos depois. | Inofensivo, mas aparece como `ERROR` no log da subida e pode assustar. | P | 3 |
| O13 | **O Redis fora do ar deixa tudo passar** (corte de sessão e limite de requisições). | É decisão consciente, mas precisa de alerta: hoje a segurança some sem ninguém ver. | M | 2 |

---

## 4. Arquitetura (a "Onda 5" da migração)

| # | Item | Por que importa | Tam. | Prio. |
| --- | --- | --- | --- | --- |
| A1 | **Padrão outbox para os eventos.** Gravar o evento na mesma transação que o dado e publicar depois. | Resolve O7: hoje o dado pode ser gravado e o evento perdido. | G | 2 |
| A2 | **Eventos tipados por serviço.** `DomainEvent` é um registro genérico com texto livre. | Quem consome precisa interpretar strings. Um tipo por evento torna o contrato verificável. | G | 3 |
| A3 | **Chamadas remotas (Feign) dentro de transação.** | Uma chamada lenta segura a conexão com o banco aberta. Mover a chamada para fora do caso de uso transacional. | M | 2 |
| A4 | **Nomes das constantes de `ErrorCode` em português**, com o valor igual ao nome (ADR 0014). | O valor é contrato; o nome não deveria ser. Renomear exige separar nome e valor em todos os serviços. | M | 3 |
| A5 | **`CheckRateLimitService` só repassa a chamada** (ADR 0012). `RedisRateLimitAdapter` ainda grava a configuração no limitador compartilhado dentro do construtor. | Se o limite ganhar regra (por usuário, por rota), a regra entra ali; até lá é indireção. | P | 3 |
| A6 | **Config Server centralizado.** Já estava no roadmap. | Hoje cada serviço tem suas propriedades. | G | 3 |

---

## 5. Processo e documentação

| # | Item | Por que importa | Tam. | Prio. |
| --- | --- | --- | --- | --- |
| D1 | **Branches de PRs já mergeados continuam no remoto.** | Poluem a lista. Só apagar com confirmação de que o PR está `MERGED`. | P | 3 |
| D2 | **Fechar a issue de acompanhamento da migração (#23).** | A migração terminou. | P | 3 |
| D3 | **Atualizar o "Mapa do Lexo" e as notas do livro**, que ficam fora do repositório. | Registram as divergências entre a prática e o livro de Hombergs. | P | 3 |
| D4 | **Remover os arquivos soltos** `scripts/oracle-retry-instance.sh` e a pasta `.claude/`, que aparecem como não rastreados. | Não pertencem ao projeto. | P | 3 |

---

## 6. Produto (do roadmap do README)

- Captura de publicações (DJe) para gerar prazos automaticamente.
- Jurimetria (relatórios e BI).
- Cobrança integrada (PIX e boleto).

---

## Ordem sugerida

1. **Antes do próximo deploy:** S1, S2, S3, O2, O3 (todos pequenos).
2. **Primeira rodada de qualidade:** T1 (teste de fumaça no CI) e T2 (porta de cobertura). Com eles o resto da lista fica mais seguro de fazer.
3. **Depois:** O1 (monitorar as filas de mortas), S5, T3, T4 e O13.
4. **Frente grande:** A1 (outbox) com O7 e O5, quando o volume ou a necessidade justificarem.
