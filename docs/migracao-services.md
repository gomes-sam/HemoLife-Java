# Migração das regras dos DAOs Flask para Services

Implementação baseada nas regras fornecidas de UsuarioDAO, OngDAO, InscricaoDAO,
UnidadeDAO e ExameDAO. Os arquivos Python originais não estão no projeto; portanto,
a equivalência validada é com essas regras, e não uma comparação linha a linha dos DAOs.

## Arquivos criados e alterados

Criados em `src/main/java/com/hemolife`:

- `service/UsuarioService.java`, `OngService.java`, `InscricaoService.java`,
  `UnidadeService.java` e `ExameService.java`.
- `service/ValidacoesNegocio.java` e `ConflitosPersistencia.java`: auxiliares internos
  de normalização, validação de obrigatórios e reconhecimento de constraints conhecidas.
- `config/PasswordConfig.java`: bean `PasswordEncoder` BCrypt com custo 12.
- `dto/UsuarioResponse.java`, `OngResponse.java`, `InscricaoResponse.java`,
  `UnidadeResponse.java` e `ExameResponse.java`.
- `exception/NegocioException.java`, `DadosObrigatoriosException.java`,
  `DadosInvalidosException.java`, `EmailJaCadastradoException.java`,
  `PerfilInvalidoException.java`, `TipoSanguineoObrigatorioException.java`,
  `UsuarioNaoEncontradoException.java`, `CnpjJaCadastradoException.java`,
  `OngNaoEncontradaException.java`, `OngEmUsoException.java`,
  `InscricaoDuplicadaException.java`, `InscricaoNaoEncontradaException.java`,
  `UsuarioNaoInscritoException.java`, `UnidadeNaoEncontradaException.java`,
  `ExameNaoEncontradoException.java`, `DataExameInvalidaException.java`,
  `HorarioInvalidoException.java` e `ConflitoAgendamentoException.java`.

Criados em `src/test/java/com/hemolife/service`:

- `UsuarioServiceTest.java`, `OngServiceTest.java`, `InscricaoServiceTest.java`,
  `UnidadeServiceTest.java`, `ExameServiceTest.java`.
- `ServicesPersistenceTest.java` e `ServiceFixtures.java`.

Alterados: os cinco repositories (somente adições), `service/package-info.java`,
`README.md` e `docs/migracao-dominio.md`. Esta documentação também é nova.
Nenhuma alteração nas entidades, adapters, Singleton, `TesteBancoController`,
`SecurityConfig`, `pom.xml` ou `application.properties` foi necessária nesta etapa.

## Contratos dos Services

| Service | Métodos públicos |
| --- | --- |
| UsuarioService | `criar(nome, email, senha, tipoSanguineo, perfil)`, `buscarPorId(id)`, `buscarPorEmail(email)` |
| OngService | `cadastrar(nome, email, senha, cnpj)`, `listar()`, `buscarPorId(id)`, `atualizar(id, nome, email, cnpj)`, `deletar(id)` |
| InscricaoService | `inscrever(usuarioId, ongId)`, `cancelar(usuarioId, ongId)`, `listarOngsDoUsuario(usuarioId)`, `listarUsuariosDaOng(ongId)`, `jaInscrito(usuarioId, ongId)` |
| UnidadeService | `listar()`, `criar(nome, telefone, endereco)`, `buscarPorId(id)` |
| ExameService | `agendar(usuarioId, ongId, unidadeId, dataExame, horario)`, `listarDoUsuario(usuarioId)`, `cancelar(usuarioId, exameId)` |

IDs usam `Long`; `dataExame` usa `LocalDate`; horário e perfil de entrada são `String`.
O perfil aceita ADMIN/DOADOR ou admin/doador, com espaços externos removidos.
Os Services retornam DTOs: não expõem as entidades nem suas senhas/hashes, inclusive
em listas de membros, ONGs e exames. `deletar` ONG e `cancelar` inscrição retornam `void`;
`jaInscrito` retorna `boolean`. A serialização dessas respostas é verificada nos testes.

## Regras migradas

**Usuario:** nome recebe trim; email recebe trim e lowercase com `Locale.ROOT`.
Nome, email e senha são obrigatórios; perfis desconhecidos têm exceção específica.
ADMIN sempre grava tipo sanguíneo nulo; DOADOR exige tipo sanguíneo preenchido,
com trim. A senha do usuário preserva seus espaços e é convertida para BCrypt.
Busca e detecção de email duplicado ignoram maiúsculas, inclusive em registros legados.

**ONG:** nome, email, senha e CNPJ recebem trim; email também recebe lowercase.
Todos são obrigatórios. Email e CNPJ devem ser únicos. Cadastro usa BCrypt.
A atualização requer nome, email e CNPJ, ignora o próprio ID nas consultas de
duplicidade e executa JPQL que altera somente esses três campos, preservando o hash.
A listagem é ordenada por nome. A exclusão inexistente lança `OngNaoEncontradaException`;
as FKs existentes impedem excluir ONG com vínculos, informando `OngEmUsoException`.

**Inscricao:** valida a existência da ONG antes do usuário e rejeita duplicidades.
O cancelamento apaga somente o registro de inscrição, sem consultar exames.
Listagens retornam DTOs; membros de ONG são ordenados por nome. A consulta
`jaInscrito` verifica o par usuário/ONG. Cancelar inscrição inexistente lança
`InscricaoNaoEncontradaException`; listas sem resultados retornam listas vazias.

**Unidade:** nome, telefone e endereço são obrigatórios e recebem trim. Listagem
ordenada por nome e busca por ID com `UnidadeNaoEncontradaException`.

**Exame:** valida primeiro os campos obrigatórios, depois busca usuário e ONG,
consulta a inscrição e busca a unidade. Só então valida data e horário. As mensagens
para referências ausentes são exatamente `ONG nao encontrada.` e
`Unidade nao encontrada.`. Datas anteriores ao dia atual são rejeitadas;
horário deve corresponder estritamente a HH:mm, sem segundos ou espaços externos.
O Service consulta o conflito por usuário/data/horário, chama `Exame.agendar(...)`
e salva com flush. ONG e unidade não fazem parte da chave de conflito.

Os exames são listados por data ascendente e horário ascendente. Cancelamento
consulta `findByIdAndUsuarioId`, tratando exame alheio como não encontrado. Chama
`exame.cancelar()` e salva, sem excluir ou revalidar inscrição. Assim, exames
históricos continuam acessíveis após cancelar uma inscrição, e exames cancelados
continuam ocupando a combinação usuário/data/horário.

## Transações e conflitos

Os Services usam `@Transactional(readOnly = true)` no nível de classe e
`@Transactional` em todos os métodos de escrita. Exceções de negócio são unchecked,
provocando rollback. Não há conversão para status HTTP nesta camada.

Consultas de existência antecipam mensagens de duplicidade, mas as constraints
continuam decidindo conflitos concorrentes. `saveAndFlush` expõe a violação dentro
do método, onde `DataIntegrityViolationException` é traduzida apenas quando o
SQLState e o nome da constraint correspondem ao conflito esperado:

| Constraint | Exceção de negócio |
| --- | --- |
| `uk_usuarios_email`, `uk_ong_email` | `EmailJaCadastradoException` |
| `uk_ong_cnpj` | `CnpjJaCadastradoException` |
| `uk_inscricao_usuario_ong` | `InscricaoDuplicadaException` |
| `uk_exames_usuario_data_horario` | `ConflitoAgendamentoException` |
| `fk_inscricao_ong`, `fk_exames_ong`, durante exclusão de ONG | `OngEmUsoException` |

Outras falhas de persistência são propagadas, sem serem rotuladas como duplicidade.
Não há catch genérico em todos os métodos. Os Services não registram senhas, hashes
ou parâmetros de entrada em logs. Mensagens de negócio não reproduzem os detalhes SQL.

## Métodos acrescentados aos repositories

| Repository | Adições |
| --- | --- |
| UsuarioRepository | `findByEmailIgnoreCase`, `existsByEmailIgnoreCase` |
| OngRepository | `existsByEmailIgnoreCase`, `existsByEmailIgnoreCaseAndIdNot`, `existsByCnpjAndIdNot`, `findAllByOrderByNomeAsc`, `atualizarDados`, `excluirPorId` |
| InscricaoRepository | `findByOngIdOrderByUsuarioNomeAsc`, com EntityGraph de usuário |
| UnidadeRepository | `findAllByOrderByNomeAsc` |
| ExameRepository | `findByIdAndUsuarioId` |

Os métodos anteriores foram mantidos. As consultas de ordenação de exames,
existência de inscrição, CNPJ e conflito de horário já existiam e foram reutilizadas.
`atualizarDados` e `excluirPorId` usam `@Modifying`, com flush antes da execução e
limpeza do contexto após a operação, preservando as entidades atuais sem adicionar setters.

## Decisões e diferenças explícitas

- Novos cadastros Java usam BCrypt com custo 12. Hashes Flask existentes não são
  convertidos, regravados nem validados para login nesta etapa.
- BCrypt tem limite de 72 bytes de entrada; senhas maiores são rejeitadas com
  `DadosInvalidosException`, sem truncamento. A contagem considera UTF-8.
- A senha de Usuario conserva espaços externos; a senha de ONG recebe trim,
  conforme a distinção nas regras fornecidas.
- “Usuário válido” foi interpretado como Usuario existente. Não foi acrescentada
  restrição a DOADOR para agendar: ADMIN existente e inscrito também pode agendar.
- CNPJ recebe trim, preservando pontuação. Não foi acrescentada validação de
  dígitos verificadores ou equivalência entre versões pontuadas e não pontuadas.
- Os DTOs são o contrato Java desta camada e não definem ainda um contrato HTTP.
- Email é consultado sem distinção de maiúsculas; novos emails são gravados em
  lowercase. As constraints existentes foram preservadas e continuam comparando
  os textos armazenados. Não houve saneamento de possíveis duplicidades legadas.
- O tratamento de conflitos usa os nomes de constraints dos mapeamentos atuais.
  Um schema Flask com outros nomes deve ser compatibilizado antes da implantação.

Não há novos controllers, login, autenticação, sessão ou upload MongoDB nesta etapa.
As rotas temporárias existentes continuam preservadas. Detalhes adicionais do
Flask só poderão ser comparados quando seu código ou regras forem disponibilizados.

## Testes

```powershell
$env:JAVA_HOME = 'C:\Users\Samuel\.jdks\ms-21.0.11'
.\mvnw.cmd verify
```

Resultado: **BUILD SUCCESS — 158 testes, zero falhas, zero erros e zero ignorados**.
São 72 testes anteriores, 75 testes unitários novos de Services e 11 testes novos
de integração dos Services com PostgreSQL real temporário.

Os cinco Services têm testes unitários. `ServicesPersistenceTest` também usa
PostgreSQL temporário e verifica BCrypt configurado, consultas reais, ordenação,
atualização sem alterar hash, inscrição cancelada com exames históricos, cancelamento
por proprietário, conflitos de horário e tradução de violações SQL reais.
O cenário de concorrência simula uma pré-consulta desatualizada e deixa a gravação
atingir a UNIQUE real; não é um teste de carga com múltiplas threads.

Os testes de Service em PostgreSQL usam `PostgresPersistenceTestConfig`, com
DataSource próprio, porta aleatória e schema descartável. A URL e as credenciais
do banco local `hemolife` não são utilizadas. Nenhuma alteração foi feita nesse banco.
Relatórios ficam em `target/surefire-reports` e o log desta execução em
`target/verificacao-services.log`.

Referências técnicas: [PasswordEncoder e BCrypt](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
e [consultas modificadoras JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).
