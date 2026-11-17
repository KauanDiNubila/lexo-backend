# Decisões de arquitetura

Cada decisão que mudou a forma de construir o Lexo está em um arquivo. A migração para a arquitetura
hexagonal seguiu o fluxo da ADR 0001.

| ADR | Decisão |
| --- | --- |
| [0001](0001-fluxo-de-trabalho-da-migracao.md) | Fluxo de trabalho da migração: um PR por serviço, testes de comportamento antes |
| [0002](0002-layout-de-pacotes-e-regras-de-dependencia.md) | Layout de pacotes e regras de dependência de cada serviço |
| [0003](0003-idioma-do-codigo-e-atalhos-do-piloto.md) | Idioma do código e atalhos decididos no piloto |
| [0004](0004-agendador-janela-unica-e-risco-no-dominio.md) | Agendador, janela única e risco no domínio (processo) |
| [0005](0005-sessao-no-caso-de-uso-e-erros-de-autenticacao.md) | Sessão no caso de uso e erros de autenticação (auth) |
| [0006](0006-corte-de-sessao-para-tokens-de-acesso.md) | Corte de sessão para tokens de acesso |
| [0007](0007-limite-de-uso-e-texto-gerado-no-ia-service.md) | Limite de uso e geração de texto (ia) |
| [0008](0008-falha-do-consumidor-de-auditoria.md) | Onde fica a política de falha do consumidor de auditoria |
| [0009](0009-mensagem-invalida-no-topico-de-eventos.md) | Mensagem inválida no tópico de eventos (Kafka) |
| [0010](0010-fila-de-e-mails-no-notificacao-service.md) | Fila de e-mails no notificacao-service |
| [0011](0011-repeticao-de-envio-de-e-mail.md) | Repetição do envio de e-mail |
| [0012](0012-gateway-em-hexagonal-e-reativo.md) | Gateway em hexagonal, mantendo o fluxo reativo |
| [0013](0013-limite-por-cliente-e-identidade-completa-no-gateway.md) | Limite por cliente atrás do proxy e identidade completa no gateway |
| [0014](0014-nomes-em-ingles-nas-bibliotecas-compartilhadas.md) | Nomes em inglês nas bibliotecas compartilhadas |
| [0016](0016-troca-e-redefinicao-de-senha.md) | Troca e redefinição de senha |
