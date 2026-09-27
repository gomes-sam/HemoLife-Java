# Migração do domínio relacional HemoLife

Esta etapa implementa o modelo descrito do backend Flask e sua persistência JPA/Hibernate.
Não executa importação de dados nem altera o banco `hemolife` durante os testes.
Os nomes das cinco tabelas e das colunas de relacionamento foram preservados.

## Arquivos

Criados em `src/main/java/com/hemolife/model`:

- `Usuario.java`, `Ong.java`, `Inscricao.java`, `Unidade.java`, `Exame.java`.
- `PerfilUsuario.java`, `StatusExame.java`.

Criados em `src/main/java/com/hemolife/repository`:

- `UsuarioRepository.java`, `OngRepository.java`, `InscricaoRepository.java`.
- `UnidadeRepository.java`, `ExameRepository.java`.

Criados para testes:

- `src/test/java/com/hemolife/model/ExameDomainTest.java`.
- `src/test/java/com/hemolife/repository/PersistenciaRelacionalTest.java`.
- `src/test/java/com/hemolife/support/PostgresPersistenceTestConfig.java`.

Documentação: este arquivo, `docs/sql/schema-postgresql.sql` e
`docs/sql/remover-fk-exames-inscricao.sql`.
Alterados: `pom.xml` (dependência de testes), `README.md` e `model/package-info.java`.
Os adapters, o Singleton, `TesteBancoController` e `application.properties` foram preservados.

## Mapeamento e relacionamentos

| Entidade | Tabela | Colunas |
| --- | --- | --- |
| Usuario | `usuarios` | `id`, `nome`, `email`, `senha`, `tipo_sanguineo`, `perfil` |
| Ong | `ong` | `id`, `nome`, `email`, `senha`, `cnpj` |
| Inscricao | `inscricao` | `id`, `usuario_id`, `ong_id` |
| Unidade | `unidades` | `id`, `nome`, `telefone`, `endereco` |
| Exame | `exames` | `id`, `usuario_id`, `ong_id`, `unidade_id`, `data_exame`, `horario`, `status`, `criado_em`, `arquivo_id` |

| Associação JPA | Cardinalidade | Colunas / FK |
| --- | --- | --- |
| Inscricao → Usuario | muitos para um | `usuario_id` / `fk_inscricao_usuario` |
| Inscricao → Ong | muitos para um | `ong_id` / `fk_inscricao_ong` |
| Exame → Usuario | muitos para um | `usuario_id` / `fk_exames_usuario` |
| Exame → Ong | muitos para um | `ong_id` / `fk_exames_ong` |
| Exame → Unidade | muitos para um | `unidade_id` / `fk_exames_unidade` |

As associações são `@ManyToOne(fetch = LAZY, optional = false)`, unidirecionais,
sem cascata de criação ou remoção. `Inscricao` representa o vínculo entre usuário
e ONG com identidade própria. Não foi criado um vínculo Unidade–Ong, pois ele
não faz parte do modelo informado.

Conforme o comportamento confirmado do Flask, `Exame` tem somente as três FKs
para usuário, ONG e unidade. Não possui campo, associação JPA ou FK para `Inscricao`.
Cancelar uma inscrição remove seu registro, mesmo quando há exames históricos,
sem excluir ou alterar os exames e seus vínculos com usuário, ONG e unidade.

`Exame.agendar(usuario, ong, unidade, dataExame, horario)` recebe esses objetos
diretamente. O `ExameService`, implementado na [etapa de Services](migracao-services.md), consulta
`InscricaoRepository.existsByUsuarioIdAndOngId(...)` ou `findByUsuarioIdAndOngId(...)`
antes de chamar a factory, dentro do fluxo transacional de agendamento.
A validação permanece no Service: as entidades e o banco não exigem uma inscrição
atual para gravar, carregar ou cancelar um exame.

### Correção de bancos que receberam a FK anterior

Remover a anotação não garante que `ddl-auto=update` elimine a constraint já
existente. Se o banco recebeu a modelagem anterior, execute nele o script
[remover-fk-exames-inscricao.sql](sql/remover-fk-exames-inscricao.sql):

```sql
ALTER TABLE IF EXISTS exames DROP CONSTRAINT IF EXISTS fk_exames_inscricao;
```

O script preserva registros, as três FKs corretas e a constraint de horário.
Pode ser executado novamente sem erro. Foi validado no PostgreSQL temporário dos
testes; não foi aplicado ao banco local `hemolife`. Para bancos novos, basta o
schema atualizado, que já não contém essa FK.

## Regras implementadas

| Regra | Implementação |
| --- | --- |
| Email único de Usuario | `uk_usuarios_email` |
| Tipo sanguíneo obrigatório para doador | Bean Validation e `ck_usuarios_doador_tipo` |
| Admin sem tipo sanguíneo | Coluna opcional; a condição do doador não se aplica |
| Perfis permitidos | Enum Java, conversor para `admin`/`doador` e `ck_usuarios_perfil` |
| Email e CNPJ únicos de Ong | `uk_ong_email` e `uk_ong_cnpj` |
| Inscrição única por usuário/ONG | `uk_inscricao_usuario_ong (usuario_id, ong_id)` |
| Inscrição exigida no momento do agendamento | Validação em `ExameService` via `InscricaoRepository`, antes de `Exame.agendar(...)` |
| Usuário, ONG e unidade precisam existir | Chaves estrangeiras obrigatórias |
| Data não pode estar no passado ao agendar | Validação na factory e novamente em `@PrePersist` |
| Horário HH:mm | Parser estrito, `LocalTime` sem segundos/nanos e `ck_exames_horario_minuto` |
| Usuário não pode duplicar data/horário | `uk_exames_usuario_data_horario (usuario_id, data_exame, horario)` |
| Cancelamento mantém o registro | `Exame.cancelar()` altera somente `status` para `CANCELADO` |
| Arquivo futuro | `arquivo_id` opcional, String de 24 caracteres hexadecimais, sem integração MongoDB |

`data_exame` não recebe uma validação temporal em toda atualização: um exame
histórico deve continuar podendo ser cancelado. Também não há CHECK SQL baseado
em `current_date`, pois registros válidos se tornam históricos com a passagem do tempo.
A proteção de data se aplica aos novos agendamentos realizados pelo domínio/JPA.
Importações SQL de dados históricos permanecem possíveis.

O cancelamento é idempotente e não usa `delete`. Como a constraint de horário é
incondicional, um exame cancelado continua ocupando o par data/horário daquele usuário.
Um eventual reagendamento que libere o horário exige outra regra explicitamente definida.

Os repositories disponibilizam consultas de email, CNPJ, vínculo usuário/ONG,
existência de agendamento e listas por usuário, ONG/status ou unidade/data.
As consultas `exists...` auxiliam as mensagens de negócio dos Services; as constraints
permanecem responsáveis por unicidade e existência das entidades referenciadas
em inserções concorrentes. A existência de inscrição é uma regra do serviço,
e não uma constraint permanente sobre exames históricos.

## Compatibilidade a confirmar antes de importar dados

O código e o DDL original do Flask não estão neste projeto. Foram preservados os
nomes e as regras fornecidos; estes detalhes não informados foram definidos para
o novo mapeamento e precisam ser comparados com o schema legado:

- IDs Java `Long`, SQL `bigint generated by default as identity`.
- `perfil` armazena exatamente `admin` ou `doador`, sem enum numérico.
- Como somente `CANCELADO` foi especificado, os status desta etapa são `AGENDADO`
  (inicial) e `CANCELADO`, gravados em maiúsculas. Outros status legados ainda
  precisam ser informados antes de importar exames que os utilizem.
- `data_exame` usa `date`; `horario` usa `time(6)` sem fuso, com segundos iguais a zero.
  `getHorarioFormatado()` fornece HH:mm; ferramentas SQL podem exibir HH:mm:ss.
- `criado_em` usa `timestamp(6)` sem fuso e é preenchido no `@PrePersist`, segundo
  o relógio/fuso da JVM. O SQL não contém um valor DEFAULT para esse campo.
- Nomes, emails e hashes de senha têm limite de 255 caracteres; CNPJ 18,
  telefone 30, endereço 500 e tipo sanguíneo 10. Consulte o SQL completo para os tipos.
- Emails e CNPJs não são normalizados; a unicidade compara o texto armazenado.
  Não há validação de dígitos verificadores de CNPJ acrescentada a esta etapa.
- `senha` preserva o texto do hash recebido do legado; os novos Services usam
  BCrypt nos cadastros Java. Não há conversão de hashes legados, login ou autenticação.

O arquivo [schema-postgresql.sql](sql/schema-postgresql.sql) é o DDL produzido
pelo Hibernate nos testes, apresentado como referência para um schema novo.
Não é um script de alteração de tabelas já preenchidas. A aplicação mantém sua
configuração atual `ddl-auto=update`; isso não substitui uma migração de dados
com revisão de tipos, duplicidades, valores legados e referências existentes.

## Verificação

```powershell
$env:JAVA_HOME = 'C:\Users\Samuel\.jdks\ms-21.0.11'
.\mvnw.cmd verify
```

Resultado da revisão de domínio anterior aos Services: **72 testes, zero falhas, zero erros, zero ignorados**.
São 20 testes existentes, 15 cenários de domínio e 37 cenários de persistência.

Os testes cobrem também a ausência da FK Exame–Inscricao, as três referências
obrigatórias, a remoção de inscrições mantendo exames históricos, a preservação
do horário após cancelamento e a execução idempotente do script de correção.

Os testes de persistência usam [Zonky Embedded Postgres](https://github.com/zonkyio/embedded-postgres),
com um servidor PostgreSQL real, diretório temporário e porta aleatória. Não exigem
Docker, credenciais de produção nem um PostgreSQL instalado. A primeira execução
baixa a dependência e seus binários pelo Maven. O servidor é encerrado ao fechar
o contexto/JVM de testes; os dados são descartados. O MongoDB não participa desses testes.

O DataSource de testes é criado diretamente a partir desse servidor; a URL da
aplicação em `localhost:5433/hemolife` não é usada. `create-drop`/`drop-and-create`
ficam restritos à configuração de testes, que ainda produz
`target/hemolife-schema-generated.sql` para inspeção.

Os casos negativos provocam erros SQL esperados para demonstrar que as constraints
funcionam. A conclusão válida da execução é `BUILD SUCCESS` com todas as asserções
aprovadas. Relatórios JUnit ficam em `target/surefire-reports`.
