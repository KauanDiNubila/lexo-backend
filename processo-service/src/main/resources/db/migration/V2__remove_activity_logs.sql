-- activity_logs era escrita a cada mudanca de processo e prazo, mas nenhum
-- endpoint jamais leu a tabela. O historico que a aplicacao mostra vem do
-- auditoria-service, alimentado pelos eventos em lexo.eventos.
drop table activity_logs;
