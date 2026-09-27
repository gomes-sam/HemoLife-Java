package com.hemolife.repository;

import com.hemolife.model.Exame;
import com.hemolife.model.Inscricao;
import com.hemolife.model.Ong;
import com.hemolife.model.PerfilUsuario;
import com.hemolife.model.StatusExame;
import com.hemolife.model.Unidade;
import com.hemolife.model.Usuario;
import com.hemolife.support.PostgresPersistenceTestConfig;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
        "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=drop-and-create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/hemolife-schema-generated.sql",
        "spring.jpa.properties.hibernate.hbm2ddl.schema-generation.script.append=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresPersistenceTestConfig.class)
class PersistenciaRelacionalTest {
    @Autowired private UsuarioRepository usuarios;
    @Autowired private OngRepository ongs;
    @Autowired private InscricaoRepository inscricoes;
    @Autowired private UnidadeRepository unidades;
    @Autowired private ExameRepository exames;
    @Autowired private TestEntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void executaEmPostgresRealEUsaSomenteAsCincoTabelasLegadas() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL");
        assertThat(jdbc.queryForList("select tablename from pg_tables where schemaname = 'public'", String.class))
                .containsExactlyInAnyOrder("usuarios", "ong", "inscricao", "unidades", "exames");
    }

    @Test
    void examePossuiApenasAsTresFksDoFlask() {
        assertThat(jdbc.queryForList("""
                select confrelid::regclass::text from pg_constraint
                where conrelid = 'exames'::regclass and contype = 'f'
                """, String.class)).containsExactlyInAnyOrder("usuarios", "ong", "unidades");
    }

    @Test
    void persisteDoadorEBuscaPorEmailComPerfilMinusculo() {
        Usuario usuario = usuarios.saveAndFlush(novoUsuario());
        entityManager.clear();
        Usuario recarregado = usuarios.findByEmail(usuario.getEmail()).orElseThrow();
        assertThat(recarregado.getId()).isEqualTo(usuario.getId());
        assertThat(recarregado.getPerfil()).isEqualTo(PerfilUsuario.DOADOR);
        assertThat(recarregado.getTipoSanguineo()).isEqualTo("O+");
        assertThat(recarregado.getSenha()).isEqualTo("hash-legado");
        assertThat(usuarios.existsByEmail(usuario.getEmail())).isTrue();
        assertThat(usuarios.existsByEmail("ausente@example.test")).isFalse();
        assertThat(jdbc.queryForObject("select perfil from usuarios where id = ?", String.class, usuario.getId()))
                .isEqualTo("doador");
    }

    @Test
    void permiteAdminSemTipoSanguineo() {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Admin", "admin@example.test", "hash-legado", null, PerfilUsuario.ADMIN));
        entityManager.clear();
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getTipoSanguineo()).isNull();
        assertThat(jdbc.queryForObject("select perfil from usuarios where id = ?", String.class, usuario.getId()))
                .isEqualTo("admin");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void validacaoJpaRejeitaDoadorSemTipoSanguineo(String tipo) {
        Usuario usuario = new Usuario("Doador", "invalido@example.test", "hash-legado", tipo, PerfilUsuario.DOADOR);
        assertThatThrownBy(() -> usuarios.saveAndFlush(usuario)).isInstanceOf(ConstraintViolationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void checkSqlTambemImpedeDoadorSemTipoSanguineo(String tipo) {
        assertThatThrownBy(() -> jdbc.update(
                "insert into usuarios(nome, email, senha, perfil, tipo_sanguineo) values (?, ?, ?, ?, ?)",
                "Doador", "invalido@example.test", "hash-legado", "doador", tipo))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_usuarios_doador_tipo");
    }

    @Test
    void bancoRejeitaPerfilForaDoFlask() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into usuarios(nome, email, senha, perfil) values (?, ?, ?, ?)",
                "Usuario", "invalido@example.test", "hash-legado", "gerente"))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_usuarios_perfil");
    }

    @Test
    void emailUsuarioEUnico() {
        Usuario usuario = usuarios.saveAndFlush(novoUsuario());
        Usuario duplicado = new Usuario("Outro", usuario.getEmail(), "hash-legado", "A+", PerfilUsuario.DOADOR);
        assertThatThrownBy(() -> usuarios.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uk_usuarios_email");
    }

    @Test
    void ongPossuiConsultasPorEmailECnpj() {
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        entityManager.clear();
        assertThat(ongs.findByEmail(ong.getEmail()).orElseThrow().getId()).isEqualTo(ong.getId());
        assertThat(ongs.findByCnpj(ong.getCnpj()).orElseThrow().getId()).isEqualTo(ong.getId());
        assertThat(ongs.existsByEmail(ong.getEmail())).isTrue();
        assertThat(ongs.existsByCnpj(ong.getCnpj())).isTrue();
        assertThat(ongs.existsByCnpj("00000000000000")).isFalse();
    }

    @Test
    void emailOngEUnico() {
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        assertThatThrownBy(() -> ongs.saveAndFlush(new Ong("Outra", ong.getEmail(), "hash-legado", "98765432000188")))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uk_ong_email");
    }

    @Test
    void cnpjOngEUnico() {
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        assertThatThrownBy(() -> ongs.saveAndFlush(novaOng(ong.getCnpj())))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uk_ong_cnpj");
    }

    @Test
    void persisteUnidadeSemCriarRelacionamentoNaoInformadoComOng() {
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        entityManager.clear();
        Unidade recarregada = unidades.findById(unidade.getId()).orElseThrow();
        assertThat(recarregada.getNome()).isEqualTo(unidade.getNome());
        assertThat(recarregada.getTelefone()).isEqualTo(unidade.getTelefone());
        assertThat(recarregada.getEndereco()).isEqualTo(unidade.getEndereco());
    }

    @Test
    void inscricaoMapeiaUsuarioEOngESuportaConsultaDoVinculo() {
        Inscricao inscricao = criarInscricao();
        Long usuarioId = inscricao.getUsuario().getId();
        Long ongId = inscricao.getOng().getId();
        entityManager.clear();
        Inscricao recarregada = inscricoes.findByUsuarioIdAndOngId(usuarioId, ongId).orElseThrow();
        assertThat(recarregada.getUsuario().getId()).isEqualTo(usuarioId);
        assertThat(recarregada.getOng().getId()).isEqualTo(ongId);
        assertThat(inscricoes.existsByUsuarioIdAndOngId(usuarioId, ongId)).isTrue();
        assertThat(inscricoes.existsByUsuarioIdAndOngId(usuarioId, -1L)).isFalse();
        assertThat(inscricoes.findByUsuarioId(usuarioId)).hasSize(1);
        assertThat(inscricoes.findByOngId(ongId)).hasSize(1);
    }

    @Test
    void impedeInscricaoDuplicadaNaMesmaOng() {
        Inscricao inscricao = criarInscricao();
        assertThatThrownBy(() -> inscricoes.saveAndFlush(new Inscricao(inscricao.getUsuario(), inscricao.getOng())))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uk_inscricao_usuario_ong");
    }

    @Test
    void permiteInscricaoDoMesmoUsuarioEmOutraOng() {
        Inscricao original = criarInscricao();
        Ong outra = ongs.saveAndFlush(novaOng("98765432000188"));
        inscricoes.saveAndFlush(new Inscricao(original.getUsuario(), outra));
        assertThat(inscricoes.findByUsuarioId(original.getUsuario().getId())).hasSize(2);
    }

    @Test
    void examePersisteTodosOsVinculosDatasEReferenciaOpcionalGridFs() {
        Inscricao inscricao = criarInscricao();
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        Exame exame = exames.saveAndFlush(Exame.agendar(inscricao.getUsuario(), inscricao.getOng(), unidade, LocalDate.now().plusDays(1), "09:30"));
        assertThat(exame.getCriadoEm()).isNotNull();
        assertThat(exame.getArquivoId()).isNull();
        exame.definirArquivoId("507f1f77bcf86cd799439011");
        exames.flush();
        entityManager.clear();

        Exame recarregado = exames.findById(exame.getId()).orElseThrow();
        assertThat(recarregado.getUsuario().getId()).isEqualTo(inscricao.getUsuario().getId());
        assertThat(recarregado.getOng().getId()).isEqualTo(inscricao.getOng().getId());
        assertThat(recarregado.getUnidade().getId()).isEqualTo(unidade.getId());
        assertThat(recarregado.getHorario()).isEqualTo(LocalTime.of(9, 30));
        assertThat(recarregado.getDataExame()).isEqualTo(exame.getDataExame());
        assertThat(recarregado.getArquivoId()).isEqualTo("507f1f77bcf86cd799439011");
        assertThat(recarregado.getStatus()).isEqualTo(StatusExame.AGENDADO);
        assertThat(jdbc.queryForObject("select status from exames where id = ?", String.class, exame.getId())).isEqualTo("AGENDADO");
    }

    @Test
    void impedeMesmoUsuarioNoMesmoHorarioMesmoEmOutraOng() {
        Inscricao primeira = criarInscricao();
        Ong outra = ongs.saveAndFlush(novaOng("98765432000188"));
        Inscricao segunda = inscricoes.saveAndFlush(new Inscricao(primeira.getUsuario(), outra));
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        exames.saveAndFlush(Exame.agendar(primeira.getUsuario(), primeira.getOng(), unidade, LocalDate.now(), "09:00"));
        assertThatThrownBy(() -> exames.saveAndFlush(Exame.agendar(segunda.getUsuario(), segunda.getOng(), unidade, LocalDate.now(), "09:00")))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uk_exames_usuario_data_horario");
    }

    @Test
    void permiteUsuariosDiferentesNoMesmoHorario() {
        Inscricao primeira = criarInscricao();
        Usuario outro = usuarios.saveAndFlush(novoUsuario());
        Inscricao segunda = inscricoes.saveAndFlush(new Inscricao(outro, primeira.getOng()));
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        exames.saveAndFlush(Exame.agendar(primeira.getUsuario(), primeira.getOng(), unidade, LocalDate.now(), "09:00"));
        exames.saveAndFlush(Exame.agendar(segunda.getUsuario(), segunda.getOng(), unidade, LocalDate.now(), "09:00"));
        assertThat(exames.count()).isEqualTo(2);
    }

    @Test
    void consultasDeExameFiltramDoadorOngUnidadeDataEStatus() {
        Inscricao inscricao = criarInscricao();
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        LocalDate data = LocalDate.now().plusDays(1);
        Exame tarde = exames.saveAndFlush(Exame.agendar(inscricao.getUsuario(), inscricao.getOng(), unidade, data, "14:00"));
        Exame cedo = exames.saveAndFlush(Exame.agendar(inscricao.getUsuario(), inscricao.getOng(), unidade, data, "09:00"));
        tarde.cancelar();
        exames.flush();
        assertThat(exames.findByUsuarioIdOrderByDataExameAscHorarioAsc(inscricao.getUsuario().getId()))
                .extracting(Exame::getId).containsExactly(cedo.getId(), tarde.getId());
        assertThat(exames.findByOngIdAndStatus(inscricao.getOng().getId(), StatusExame.CANCELADO))
                .extracting(Exame::getId).containsExactly(tarde.getId());
        assertThat(exames.findByUnidadeIdAndDataExame(unidade.getId(), data)).hasSize(2);
        assertThat(exames.existsByUsuarioIdAndDataExameAndHorario(inscricao.getUsuario().getId(), data, LocalTime.of(9, 0))).isTrue();
        assertThat(exames.existsByUsuarioIdAndDataExameAndHorario(inscricao.getUsuario().getId(), data, LocalTime.of(10, 0))).isFalse();
    }

    @Test
    void cancelarPreservaRegistroMesmoQuandoDataJaPassou() {
        Exame exame = criarExame();
        jdbc.update("update exames set data_exame = ? where id = ?", LocalDate.now().minusDays(1), exame.getId());
        var criadoEm = jdbc.queryForObject("select criado_em from exames where id = ?", java.sql.Timestamp.class, exame.getId());
        entityManager.clear();

        exames.findById(exame.getId()).orElseThrow().cancelar();
        exames.flush();
        entityManager.clear();

        assertThat(exames.count()).isEqualTo(1);
        Exame cancelado = exames.findById(exame.getId()).orElseThrow();
        assertThat(cancelado.getStatus()).isEqualTo(StatusExame.CANCELADO);
        assertThat(cancelado.getDataExame()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(cancelado.getCriadoEm()).isEqualTo(criadoEm.toLocalDateTime());
    }

    @Test
    void cancelamentoNaoLiberaUniqueConstraintDoHorario() {
        Exame exame = criarExame();
        exame.cancelar();
        exames.flush();
        assertThatThrownBy(() -> exames.saveAndFlush(Exame.agendar(exame.getUsuario(), exame.getOng(), exame.getUnidade(), exame.getDataExame(), exame.getHorario())))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("uk_exames_usuario_data_horario");
    }

    @Test
    void persistenciaNaoValidaInscricaoPoisRegraPertenceAoFuturoServico() {
        Usuario usuario = usuarios.saveAndFlush(novoUsuario());
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        Exame exame = exames.saveAndFlush(Exame.agendar(usuario, ong, unidade, LocalDate.now(), "09:00"));
        entityManager.clear();

        assertThat(inscricoes.existsByUsuarioIdAndOngId(usuario.getId(), ong.getId())).isFalse();
        Exame recarregado = exames.findById(exame.getId()).orElseThrow();
        assertThat(recarregado.getUsuario().getId()).isEqualTo(usuario.getId());
        assertThat(recarregado.getOng().getId()).isEqualTo(ong.getId());
    }

    @Test
    void bancoImpedeUsuarioInexistenteNoExame() {
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        assertThatThrownBy(() -> inserirExameSql(-1L, ong.getId(), unidade.getId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("fk_exames_usuario");
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuario_id", "ong_id", "unidade_id"})
    void bancoExigeRelacionamentosNaoNulos(String coluna) {
        Exame exame = criarExame();
        assertThatThrownBy(() -> jdbc.update("update exames set " + coluna + " = null where id = ?", exame.getId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining(coluna);
    }

    @Test
    void bancoImpedeOngInexistenteNoExame() {
        Inscricao inscricao = criarInscricao();
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        assertThatThrownBy(() -> inserirExameSql(inscricao.getUsuario().getId(), -1L, unidade.getId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("fk_exames_ong");
    }

    @Test
    void bancoImpedeUnidadeInexistenteNoExame() {
        Inscricao inscricao = criarInscricao();
        assertThatThrownBy(() -> inserirExameSql(inscricao.getUsuario().getId(), inscricao.getOng().getId(), -1L))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("fk_exames_unidade");
    }

    @Test
    void bancoImpedeSegundosNoHorario() {
        Exame exame = criarExame();
        assertThatThrownBy(() -> jdbc.update("update exames set horario = time '09:00:01' where id = ?", exame.getId()))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_exames_horario_minuto");
    }

    @Test
    void bancoImpedeStatusDesconhecido() {
        Exame exame = criarExame();
        assertThatThrownBy(() -> jdbc.update("update exames set status = 'DESCONHECIDO' where id = ?", exame.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @EnumSource(StatusExame.class)
    void excluirInscricaoPreservaExameHistoricoEPermiteSeuCancelamento(StatusExame status) {
        Inscricao inscricao = criarInscricao();
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        Exame exame = Exame.agendar(inscricao.getUsuario(), inscricao.getOng(), unidade, LocalDate.now(), "09:00");
        exame.definirArquivoId("507f1f77bcf86cd799439011");
        if (status == StatusExame.CANCELADO) {
            exame.cancelar();
        }
        exames.saveAndFlush(exame);
        LocalDate dataHistorica = LocalDate.now().minusDays(1);
        jdbc.update("update exames set data_exame = ? where id = ?", dataHistorica, exame.getId());
        entityManager.clear();

        inscricoes.deleteById(inscricao.getId());
        inscricoes.flush();
        entityManager.clear();

        assertThat(inscricoes.existsById(inscricao.getId())).isFalse();
        assertThat(exames.count()).isEqualTo(1);
        Exame historico = exames.findById(exame.getId()).orElseThrow();
        assertThat(historico.getUsuario().getId()).isEqualTo(inscricao.getUsuario().getId());
        assertThat(historico.getOng().getId()).isEqualTo(inscricao.getOng().getId());
        assertThat(historico.getUnidade().getId()).isEqualTo(unidade.getId());
        assertThat(historico.getDataExame()).isEqualTo(dataHistorica);
        assertThat(historico.getStatus()).isEqualTo(status);
        assertThat(historico.getArquivoId()).isEqualTo("507f1f77bcf86cd799439011");

        historico.cancelar();
        exames.flush();
        entityManager.clear();
        assertThat(exames.findById(exame.getId()).orElseThrow().getStatus()).isEqualTo(StatusExame.CANCELADO);
    }

    @Test
    void scriptRemoveFkAntigaDeFormaIdempotentePreservandoDados() {
        Inscricao inscricao = criarInscricao();
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        Exame exame = exames.saveAndFlush(Exame.agendar(
                inscricao.getUsuario(), inscricao.getOng(), unidade, LocalDate.now(), "09:00"));
        jdbc.execute("""
                alter table exames add constraint fk_exames_inscricao
                foreign key (ong_id, usuario_id) references inscricao (ong_id, usuario_id)
                """);

        jdbc.execute((ConnectionCallback<Void>) connection -> {
            var script = new FileSystemResource("docs/sql/remover-fk-exames-inscricao.sql");
            ScriptUtils.executeSqlScript(connection, script);
            ScriptUtils.executeSqlScript(connection, script);
            return null;
        });

        inscricoes.deleteById(inscricao.getId());
        inscricoes.flush();
        entityManager.clear();
        assertThat(inscricoes.existsById(inscricao.getId())).isFalse();
        assertThat(exames.findById(exame.getId())).isPresent();
        assertThat(jdbc.queryForList("""
                select conname from pg_constraint where conrelid = 'exames'::regclass
                """, String.class))
                .doesNotContain("fk_exames_inscricao")
                .contains("fk_exames_usuario", "fk_exames_ong", "fk_exames_unidade", "uk_exames_usuario_data_horario");
    }

    private Usuario novoUsuario() {
        return new Usuario("Doador", UUID.randomUUID() + "@example.test", "hash-legado", "O+", PerfilUsuario.DOADOR);
    }

    private Ong novaOng(String cnpj) {
        return new Ong("ONG", UUID.randomUUID() + "@example.test", "hash-legado", cnpj);
    }

    private Unidade novaUnidade() {
        return new Unidade("Unidade central", "11999999999", "Rua de teste, 1");
    }

    private Inscricao criarInscricao() {
        Usuario usuario = usuarios.saveAndFlush(novoUsuario());
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        return inscricoes.saveAndFlush(new Inscricao(usuario, ong));
    }

    private Exame criarExame() {
        Usuario usuario = usuarios.saveAndFlush(novoUsuario());
        Ong ong = ongs.saveAndFlush(novaOng("12345678000199"));
        Unidade unidade = unidades.saveAndFlush(novaUnidade());
        return exames.saveAndFlush(Exame.agendar(usuario, ong, unidade, LocalDate.now().plusDays(1), "09:00"));
    }

    private void inserirExameSql(Long usuarioId, Long ongId, Long unidadeId) {
        jdbc.update("""
                insert into exames(usuario_id, ong_id, unidade_id, data_exame, horario, status, criado_em)
                values (?, ?, ?, current_date + 1, time '09:00', 'AGENDADO', current_timestamp)
                """, usuarioId, ongId, unidadeId);
    }
}
