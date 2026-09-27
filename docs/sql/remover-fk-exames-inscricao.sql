-- Correcao para bancos que receberam a FK da modelagem anterior.
-- Preserva todos os registros, as outras FKs e a UNIQUE de usuario/data/horario.
-- Executar no banco HemoLife; ddl-auto=update nao remove a constraint antiga.
ALTER TABLE IF EXISTS exames DROP CONSTRAINT IF EXISTS fk_exames_inscricao;
