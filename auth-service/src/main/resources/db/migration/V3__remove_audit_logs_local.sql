-- audit_logs local nunca foi lido por ninguem: a tela de Auditoria consulta
-- so o auditoria-service, alimentado pelo Kafka. As acoes do auth-service
-- agora publicam EventoDominio em lexo.eventos como os outros servicos.
drop table audit_logs;
