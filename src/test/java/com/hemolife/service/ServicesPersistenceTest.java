package com.hemolife.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemolife.config.PasswordConfig;
import com.hemolife.exception.*;
import com.hemolife.model.*;
import com.hemolife.repository.*;
import com.hemolife.support.PostgresPersistenceTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresPersistenceTestConfig.class, PasswordConfig.class, UsuarioService.class,
        OngService.class, InscricaoService.class, UnidadeService.class, ExameService.class})
class ServicesPersistenceTest {
    @Autowired private UsuarioService usuarioService;
    @Autowired private OngService ongService;
    @Autowired private InscricaoService inscricaoService;
    @Autowired private UnidadeService unidadeService;
    @Autowired private ExameService exameService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private OngRepository ongs;
    @Autowired private InscricaoRepository inscricoes;
    @Autowired private UnidadeRepository unidades;
    @Autowired private ExameRepository exames;
    @Autowired private PasswordEncoder encoder;
    @Autowired private TestEntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void novosCadastrosUsamBCryptConfiguradoERespostasNaoIncluemCredenciais() throws Exception {
        var usuario = usuarioService.criar(" Ana ", " ANA@EXAMPLE.TEST ", " senha usuario ", "O+", "ADMIN");
        var ong = ongService.cadastrar(" ONG ", " ONG@EXAMPLE.TEST ", " senha ong ", " 12345678000199 ");
        entityManager.clear();

        String hashUsuario = usuarios.findById(usuario.id()).orElseThrow().getSenha();
        String hashOng = ongs.findById(ong.id()).orElseThrow().getSenha();
        assertThat(hashUsuario).startsWith("$2a$12$");
        assertThat(hashOng).startsWith("$2a$12$");
        assertThat(encoder.matches(" senha usuario ", hashUsuario)).isTrue();
        assertThat(encoder.matches("senha ong", hashOng)).isTrue();
        assertThat(usuarios.findById(usuario.id()).orElseThrow().getTipoSanguineo()).isNull();
        assertThat(usuarioService.buscarPorEmail(" ANA@EXAMPLE.TEST ").id()).isEqualTo(usuario.id());
        var mapper = new ObjectMapper();
        assertThat(mapper.writeValueAsString(usuario)).doesNotContain("senha", hashUsuario);
        assertThat(mapper.writeValueAsString(ong)).doesNotContain("senha", hashOng);
    }

    @Test
    void atualizarOngIgnoraProprioIdEPreservaHashLegado() {
        Ong ong = salvarOng("Antes", "12345678000199");
        String hashAntes = ong.getSenha();
        var atualizada = ongService.atualizar(ong.getId(), " Depois ", " " + ong.getEmail().toUpperCase() + " ", " " + ong.getCnpj() + " ");
        assertThat(atualizada.nome()).isEqualTo("Depois");
        entityManager.clear();
        Ong recarregada = ongs.findById(ong.getId()).orElseThrow();
        assertThat(recarregada.getNome()).isEqualTo("Depois");
        assertThat(recarregada.getEmail()).isEqualTo(ong.getEmail());
        assertThat(recarregada.getSenha()).isEqualTo(hashAntes);
    }

    @Test
    void consultasDeConflitoDaAtualizacaoDetectamOutraOng() {
        Ong primeira = salvarOng("Primeira", "12345678000199");
        Ong outra = salvarOng("Outra", "98765432000188");
        assertThat(ongs.existsByEmailIgnoreCaseAndIdNot(outra.getEmail().toUpperCase(), primeira.getId())).isTrue();
        assertThat(ongs.existsByCnpjAndIdNot(outra.getCnpj(), primeira.getId())).isTrue();
        assertThatThrownBy(() -> ongService.atualizar(primeira.getId(), "Novo nome", outra.getEmail(), primeira.getCnpj()))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void emailLegadoComMaiusculasImpedeNovoCadastro() {
        usuarios.saveAndFlush(new Usuario("Legado", "ANA@EXAMPLE.TEST", "hash-legado", "O+", PerfilUsuario.DOADOR));
        assertThat(usuarioService.buscarPorEmail("ana@example.test").nome()).isEqualTo("Legado");
        assertThatThrownBy(() -> usuarioService.criar("Nova", "ana@example.test", "segredo", "A+", "DOADOR"))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void consultasOrdenamOngsUnidadesMembrosEExamesNoPostgres() throws Exception {
        Usuario zoe = salvarUsuario("Zoe");
        Usuario ana = salvarUsuario("Ana");
        Ong zulu = salvarOng("Zulu", "12345678000199");
        salvarOng("Alfa", "98765432000188");
        var unidadeZulu = unidadeService.criar("Zulu", "11999999999", "Rua Z");
        unidadeService.criar("Alfa", "11888888888", "Rua A");
        inscricaoService.inscrever(zoe.getId(), zulu.getId());
        inscricaoService.inscrever(ana.getId(), zulu.getId());
        assertThat(ongService.listar()).extracting(r -> r.nome()).containsExactly("Alfa", "Zulu");
        assertThat(unidadeService.listar()).extracting(r -> r.nome()).containsExactly("Alfa", "Zulu");
        assertThat(inscricaoService.listarUsuariosDaOng(zulu.getId())).extracting(r -> r.nome()).containsExactly("Ana", "Zoe");
        assertThat(inscricaoService.listarOngsDoUsuario(ana.getId())).extracting(r -> r.id()).containsExactly(zulu.getId());

        LocalDate data = LocalDate.now().plusDays(1);
        var tarde = exameService.agendar(ana.getId(), zulu.getId(), unidadeZulu.id(), data, "14:00");
        var amanha = exameService.agendar(ana.getId(), zulu.getId(), unidadeZulu.id(), data.plusDays(1), "08:00");
        var cedo = exameService.agendar(ana.getId(), zulu.getId(), unidadeZulu.id(), data, "09:00");
        assertThat(cedo.criadoEm()).isNotNull();
        assertThat(exameService.listarDoUsuario(ana.getId())).extracting(r -> r.id())
                .containsExactly(cedo.id(), tarde.id(), amanha.id());
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(exameService.listarDoUsuario(ana.getId())))
                .doesNotContain("senha", "hash-legado");
    }

    @Test
    void cancelarInscricaoPreservaHistoricoQuePodeSerCanceladoSemNovaInscricao() {
        Usuario usuario = salvarUsuario("Ana");
        Ong ong = salvarOng("ONG", "12345678000199");
        Unidade unidade = salvarUnidade("Centro");
        inscricaoService.inscrever(usuario.getId(), ong.getId());
        var exame = exameService.agendar(usuario.getId(), ong.getId(), unidade.getId(), LocalDate.now(), "09:00");
        jdbc.update("update exames set data_exame = ? where id = ?", LocalDate.now().minusDays(1), exame.id());
        entityManager.clear();

        inscricaoService.cancelar(usuario.getId(), ong.getId());
        assertThat(inscricaoService.jaInscrito(usuario.getId(), ong.getId())).isFalse();
        assertThat(exames.findById(exame.id())).isPresent();
        assertThat(exameService.cancelar(usuario.getId(), exame.id()).status()).isEqualTo(StatusExame.CANCELADO);
        entityManager.clear();
        assertThat(exames.count()).isEqualTo(1);
        assertThat(exames.findById(exame.id()).orElseThrow().getDataExame()).isEqualTo(LocalDate.now().minusDays(1));
    }

    @Test
    void exameCanceladoOcupaHorarioMesmoEmOutraOngEUnidade() {
        Usuario usuario = salvarUsuario("Ana");
        Ong primeira = salvarOng("Primeira", "12345678000199");
        Ong segunda = salvarOng("Segunda", "98765432000188");
        Unidade centro = salvarUnidade("Centro");
        Unidade outra = salvarUnidade("Outra");
        inscricaoService.inscrever(usuario.getId(), primeira.getId());
        inscricaoService.inscrever(usuario.getId(), segunda.getId());
        LocalDate data = LocalDate.now().plusDays(1);
        var exame = exameService.agendar(usuario.getId(), primeira.getId(), centro.getId(), data, "09:00");
        exameService.cancelar(usuario.getId(), exame.id());
        assertThatThrownBy(() -> exameService.agendar(usuario.getId(), segunda.getId(), outra.getId(), data, "09:00"))
                .isInstanceOf(ConflitoAgendamentoException.class);
    }

    @Test
    void naoCancelaExameDeOutroUsuario() {
        Usuario dono = salvarUsuario("Dono");
        Usuario outro = salvarUsuario("Outro");
        Ong ong = salvarOng("ONG", "12345678000199");
        Unidade unidade = salvarUnidade("Centro");
        inscricaoService.inscrever(dono.getId(), ong.getId());
        var exame = exameService.agendar(dono.getId(), ong.getId(), unidade.getId(), LocalDate.now(), "09:00");
        assertThat(exames.findByIdAndUsuarioId(exame.id(), outro.getId())).isEmpty();
        assertThatThrownBy(() -> exameService.cancelar(outro.getId(), exame.id()))
                .isInstanceOf(ExameNaoEncontradoException.class);
        assertThat(exames.findById(exame.id()).orElseThrow().getStatus()).isEqualTo(StatusExame.AGENDADO);
    }

    @Test
    void traduzViolacaoRealDaUniqueSePreConsultaNaoDetectarConflito() {
        Usuario usuario = salvarUsuario("Ana");
        Ong ong = salvarOng("ONG", "12345678000199");
        Unidade unidade = salvarUnidade("Centro");
        inscricaoService.inscrever(usuario.getId(), ong.getId());
        LocalDate data = LocalDate.now().plusDays(1);
        exameService.agendar(usuario.getId(), ong.getId(), unidade.getId(), data, "09:00");

        ExameRepository consultaDesatualizada = mock(ExameRepository.class, delegatesTo(exames));
        doReturn(false).when(consultaDesatualizada)
                .existsByUsuarioIdAndDataExameAndHorario(usuario.getId(), data, LocalTime.of(9, 0));
        ExameService service = new ExameService(consultaDesatualizada, usuarios, ongs, inscricoes, unidades);
        assertThatThrownBy(() -> service.agendar(usuario.getId(), ong.getId(), unidade.getId(), data, "09:00"))
                .isInstanceOf(ConflitoAgendamentoException.class);
    }

    @Test
    void traduzFkRealAoTentarDeletarOngEmUso() {
        Usuario usuario = salvarUsuario("Ana");
        Ong ong = salvarOng("ONG", "12345678000199");
        inscricaoService.inscrever(usuario.getId(), ong.getId());
        assertThatThrownBy(() -> ongService.deletar(ong.getId())).isInstanceOf(OngEmUsoException.class);
    }

    @Test
    void excluiOngSemVinculosEInformaQuandoNaoExiste() {
        Ong ong = salvarOng("ONG", "12345678000199");
        ongService.deletar(ong.getId());
        assertThat(ongs.findById(ong.getId())).isEmpty();
        assertThatThrownBy(() -> ongService.deletar(ong.getId())).isInstanceOf(OngNaoEncontradaException.class);
    }

    private Usuario salvarUsuario(String nome) {
        return usuarios.saveAndFlush(new Usuario(nome, UUID.randomUUID() + "@example.test", "hash-legado", "O+", PerfilUsuario.DOADOR));
    }

    private Ong salvarOng(String nome, String cnpj) {
        return ongs.saveAndFlush(new Ong(nome, UUID.randomUUID() + "@example.test", "hash-legado", cnpj));
    }

    private Unidade salvarUnidade(String nome) {
        return unidades.saveAndFlush(new Unidade(nome, "11999999999", "Rua de teste"));
    }
}
