# HemoLife — backend Java

Backend com **Java 21**, **Spring Boot 3.5.16** e **Maven**, com domínio relacional
migrado a partir das entidades e regras informadas do sistema Flask.
Inclui Spring Web, Spring Data JPA, driver PostgreSQL, Spring Data MongoDB,
Spring Security, Validation e Lombok. Os testes usam Spring Boot Test e Spring Security Test.

## Estrutura criada

```text
pom.xml
mvnw / mvnw.cmd
.mvn/wrapper/maven-wrapper.properties
docs/legacy/Main.java.original
src/main/java/com/hemolife/
├── HemoLifeApplication.java
├── controller/
│   ├── TesteController.java
│   └── TesteBancoController.java
├── service/
│   ├── UsuarioService.java
│   ├── OngService.java
│   ├── InscricaoService.java
│   ├── UnidadeService.java
│   ├── ExameService.java
│   ├── ValidacoesNegocio.java
│   ├── ConflitosPersistencia.java
│   └── package-info.java
├── repository/
│   ├── UsuarioRepository.java
│   ├── OngRepository.java
│   ├── InscricaoRepository.java
│   ├── UnidadeRepository.java
│   ├── ExameRepository.java
│   └── package-info.java
├── model/
│   ├── Usuario.java
│   ├── Ong.java
│   ├── Inscricao.java
│   ├── Unidade.java
│   ├── Exame.java
│   ├── PerfilUsuario.java
│   ├── StatusExame.java
│   └── package-info.java
├── dto/
│   ├── MensagemResponse.java
│   ├── TesteBancoResponse.java
│   ├── UsuarioResponse.java
│   ├── OngResponse.java
│   ├── InscricaoResponse.java
│   ├── UnidadeResponse.java
│   └── ExameResponse.java
├── config/
│   ├── DatabaseConfig.java
│   ├── PasswordConfig.java
│   └── SecurityConfig.java
├── database/
│   ├── DatabaseAdapter.java
│   ├── DatabaseManager.java
│   ├── PostgresAdapter.java
│   └── MongoAdapter.java
└── exception/ (conexao e excecoes de negocio descritas em docs/migracao-services.md)
src/main/resources/application.properties
src/test/java/com/hemolife/
├── controller/
│   ├── TesteControllerTest.java
│   └── TesteBancoControllerTest.java
├── config/DatabaseConfigTest.java
├── model/ExameDomainTest.java
├── repository/PersistenciaRelacionalTest.java
├── support/PostgresPersistenceTestConfig.java
├── service/ (testes unitarios dos cinco Services e ServicesPersistenceTest)
└── database/
    ├── DatabaseManagerTest.java
    ├── PostgresAdapterTest.java
    └── MongoAdapterTest.java
```

O antigo `src/Main.java` era apenas o exemplo do IntelliJ. Seu conteúdo foi preservado
integralmente em `docs/legacy/Main.java.original`, fora das fontes compiladas pelo Maven.
A entrada agora é `HemoLifeApplication`. O `.gitignore` recebeu regras para `target/`
e arquivos locais de credenciais. As configurações existentes do IntelliJ foram preservadas.
Os cinco Services implementam as regras informadas dos DAOs Flask, com transações,
novas senhas em BCrypt e DTOs de retorno sem credenciais. Consulte os contratos,
exceções e decisões em [docs/migracao-services.md](docs/migracao-services.md).

A etapa de domínio/persistência está detalhada em
[docs/migracao-dominio.md](docs/migracao-dominio.md), incluindo decisões de compatibilidade,
constraints, testes e o [SQL esperado](docs/sql/schema-postgresql.sql).
`Exame` referencia somente usuário, ONG e unidade. Excluir uma inscrição preserva
os exames históricos; `ExameService` verifica a inscrição no momento do agendamento.
Se o banco recebeu a FK composta da modelagem anterior, aplique a
[correção da constraint](docs/sql/remover-fk-exames-inscricao.sql).

## Bancos de dados

- **PostgreSQL:** `jdbc:postgresql://localhost:5433/hemolife`, destinado a usuários,
  ONGs, unidades, inscrições e exames, por meio das entidades e dos repositories JPA.
- **MongoDB:** `mongodb://localhost:27017/hemolife`, destinado aos arquivos binários
  (PDFs, exames anexados, imagens, documentos e comprovantes) via **GridFS**.
  O Spring Boot disponibiliza `GridFsTemplate` automaticamente. Sem um bucket explícito
  na configuração atual, usa o padrão `fs`, com as coleções `fs.files` e `fs.chunks`,
  criadas no primeiro armazenamento.
  Futuramente, serviços poderão receber `GridFsTemplate` por injeção de dependência;
  os registros relacionais poderão guardar o identificador do arquivo correspondente.

As entidades, repositories e Services estão implementados; não há controllers de negócio,
autenticação ou upload. Na configuração atual, o Hibernate usa `ddl-auto=update`,
que pode criar ou atualizar o schema das cinco entidades ao iniciar a aplicação.
O banco PostgreSQL precisa existir antes de iniciar a aplicação.

## Configuração por ambiente

| Variável | Uso | Padrão |
| --- | --- | --- |
| `POSTGRES_USER` | Usuário PostgreSQL | Obrigatória, sem valor no projeto |
| `POSTGRES_PASSWORD` | Senha PostgreSQL | Obrigatória, sem valor no projeto |
| `SPRING_DATASOURCE_URL` | Sobrescreve a URL JDBC configurada | `jdbc:postgresql://localhost:5433/hemolife` |
| `MONGODB_URI` | URI MongoDB, incluindo autenticação se necessária | `mongodb://localhost:27017/hemolife` |

Para MongoDB com autenticação, defina `MONGODB_URI` com usuário, senha e `authSource`
adequados à sua instalação. Codifique caracteres especiais das credenciais na URI.
Nenhuma credencial é incluída no código ou em arquivos de exemplo.
O Spring Boot não carrega arquivos `.env` automaticamente: configure as variáveis
no terminal ou na configuração de execução do IntelliJ.

## Executar no Windows / PowerShell

1. Instale ou selecione um **JDK 21** e configure `JAVA_HOME` para sua pasta.
   Nesta máquina há um JDK em `C:\Users\Samuel\.jdks\ms-21.0.11`:

   ```powershell
   $env:JAVA_HOME = 'C:\Users\Samuel\.jdks\ms-21.0.11'
   .\mvnw.cmd -version
   ```

2. Inicie PostgreSQL na porta **5433** e MongoDB na porta **27017**. Em uma sessão
   PostgreSQL com permissão de criação (por exemplo, pelo pgAdmin), crie o banco
   caso ainda não exista:

   ```sql
   CREATE DATABASE hemolife;
   ```

   O usuário da aplicação deve ter permissão de conexão e acesso ao schema desse banco.

3. Na raiz do projeto, forneça as credenciais sem gravá-las no código nem no histórico:

   ```powershell
   $credencial = Get-Credential -Message 'Credenciais do PostgreSQL para o HemoLife'
   $env:POSTGRES_USER = $credencial.UserName
   $env:POSTGRES_PASSWORD = $credencial.GetNetworkCredential().Password
   # Opcional: solicite a URI caso seu MongoDB tenha autenticacao ou outro endereco.
   # $env:MONGODB_URI = Read-Host 'URI completa do MongoDB'
   .\mvnw.cmd spring-boot:run
   ```

   Mantenha esse terminal aberto. Use `Ctrl+C` para parar a aplicação.
   O Maven Wrapper baixa o Maven e as dependências na primeira execução; é necessário
   acesso à internet nessa etapa. Não é preciso instalar Maven globalmente.

4. Em outro terminal, consulte:

   ```powershell
   Invoke-RestMethod -Uri 'http://localhost:5000/api/teste' | ConvertTo-Json -Compress
   ```

   Resposta HTTP **200**, com `Content-Type: application/json`:

   ```json
   {"mensagem":"Backend HemoLife Java funcionando"}
   ```

Essa rota testa a camada HTTP; ela não executa verificações de saúde dos bancos.
Na inicialização normal, o JPA precisa conectar ao PostgreSQL. O cliente MongoDB
pode iniciar mesmo com o servidor indisponível; `verificarConexao()` permite testar
a conexão explicitamente e informa a falha com `DatabaseConnectionException`.

### Testar as conexões reais pelo navegador

Com os bancos configurados e a aplicação iniciada, abra as rotas públicas temporárias:

- [http://localhost:5000/api/teste-postgres](http://localhost:5000/api/teste-postgres)
- [http://localhost:5000/api/teste-mongo](http://localhost:5000/api/teste-mongo)

O `TesteBancoController` recebe o `DatabaseManager` Singleton registrado por `DatabaseConfig`,
obtém o adapter pelo nome (`postgres` ou `mongo`) e executa `verificarConexao()` a cada
requisição. O PostgreSQL usa uma conexão JDBC real; o MongoDB recebe um comando `ping` real.

Respostas de sucesso, com HTTP **200**:

```json
{"success":true,"banco":"PostgreSQL","mensagem":"PostgreSQL conectado"}
```

```json
{"success":true,"banco":"MongoDB","mensagem":"MongoDB conectado"}
```

Se o adapter indicar falha, a rota retorna HTTP **500**, com uma mensagem simples:

```json
{"success":false,"banco":"PostgreSQL","mensagem":"Erro ao conectar ao PostgreSQL"}
```

Para MongoDB, os campos `banco` e `mensagem` usam `MongoDB`. A resposta não inclui
a exceção original, credenciais ou stack trace. No navegador, abra as ferramentas do
desenvolvedor (F12), selecione **Rede / Network** e recarregue a página para ver o status HTTP.

A aplicação precisa ter iniciado para responder às rotas. Se o PostgreSQL impedir a
inicialização do JPA, corrija a conexão antes de testar pelo navegador. Uma indisponibilidade
durante a execução será apresentada pela rota como HTTP 500, após o timeout do driver/pool.
Essas rotas são temporárias; sua remoção posterior deve incluir os caminhos públicos
adicionados em `SecurityConfig`. A rota `/api/teste` mantém a resposta original.

### Pelo IntelliJ IDEA

Abra o `pom.xml` como projeto ou use **Add as Maven Project** e recarregue o Maven.
Selecione **JDK 21** no Project SDK e no Maven Runner; a configuração anterior usava Java 23.
Configure as variáveis de ambiente na execução de `HemoLifeApplication` e execute seu `main`.
O Maven define `src/main/java`, `src/main/resources` e `src/test/java` como raízes apropriadas.

### Testes e JAR executável

```powershell
.\mvnw.cmd clean verify
# Para executar o JAR, mantenha as variaveis de ambiente e os bancos configurados:
& "$env:JAVA_HOME\bin\java.exe" -jar target\hemolife-0.0.1-SNAPSHOT.jar
```

Os testes automatizados não exigem bancos externos nem credenciais reais. Os testes
de persistência iniciam um PostgreSQL real temporário, isolado da configuração da aplicação,
para verificar entidades, repositories, Services, relacionamentos e constraints.
Os demais testes validam regras dos Services, domínio, JSON e acesso HTTP, Singleton,
o registro dos adapters e o tratamento de falhas de conexão.
Os testes das duas rotas de banco exercitam os adapters reais registrados no Singleton,
simulando somente o `DataSource`/`Connection` e o `MongoTemplate`. Cobrem sucesso, HTTP 500
sem detalhes internos e bloqueio de POST mesmo com usuário e token CSRF válidos.
As rotas de diagnóstico continuam testadas com clientes simulados; os testes de
persistência não validam a configuração de conexão dos seus bancos locais ou o MongoDB.
Em Linux/macOS, use `sh mvnw` no lugar de `.\mvnw.cmd` e exporte as mesmas variáveis.

## Padrões de projeto

**Adapter:** `DatabaseAdapter` define `verificarConexao()`. `PostgresAdapter` adapta
o `DataSource` JDBC, usando `Connection.isValid()` e devolvendo a conexão ao pool;
`MongoAdapter` adapta `MongoTemplate`, usando o comando `ping`. Ambos traduzem falhas
para `DatabaseConnectionException`. A interface cobre a operação comum de conectividade;
JPA e GridFS mantêm APIs próprias para seus tipos de dados. O Spring gerencia os clientes
e pools, inclusive seu encerramento.

**Singleton:** `DatabaseManager` é `final`, tem o atributo `private static final INSTANCE`,
construtor `private` e método `public static getInstance()`. A inicialização estática
garante uma instância por classloader e o `ConcurrentHashMap` protege o registro
durante acessos concorrentes. O padrão está explícito em Java, independente do escopo
singleton dos beans Spring.

`DatabaseConfig` registra os adapters automaticamente durante a criação do contexto:

```java
DatabaseManager manager = DatabaseManager.getInstance();
manager.registrar("postgres", postgresAdapter);
manager.registrar("mongo", mongoAdapter);

DatabaseAdapter postgres = manager.obter("postgres");
DatabaseAdapter mongo = manager.obter("mongo");
postgres.verificarConexao();
mongo.verificarConexao();
```

No exemplo, `postgresAdapter` e `mongoAdapter` são as instâncias injetadas pelo Spring.
O registro não executa os testes de conexão automaticamente. Um novo registro com
o mesmo nome substitui o adapter anterior. Nomes são sensíveis a maiúsculas;
nomes vazios, adapters nulos e consultas a nomes inexistentes são rejeitados.

## Segurança nesta etapa

Os GETs `/api/teste`, `/api/teste-postgres` e `/api/teste-mongo` são públicos.
As demais requisições são negadas, inclusive para
usuários autenticados, até serem definidas as regras de autorização. Não há login,
HTTP Basic ou usuário padrão gerado. A aplicação usa sessões stateless e mantém
a proteção CSRF padrão do Spring Security. `PasswordConfig` fornece apenas o BCrypt
para os novos cadastros. Autenticação e controllers de negócio permanecem para uma próxima etapa.

## Referências

- [Requisitos do Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [GridFS no Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/mongodb/template-gridfs.html)
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/)
