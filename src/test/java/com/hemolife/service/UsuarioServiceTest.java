package com.hemolife.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemolife.exception.*;
import com.hemolife.model.PerfilUsuario;
import com.hemolife.model.Usuario;
import com.hemolife.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock private UsuarioRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(repository, encoder);
    }

    @Test
    void normalizaNomeEmailEGravaBCryptSemRetornarSenha() throws Exception {
        when(repository.saveAndFlush(any())).thenAnswer(invocacao -> ServiceFixtures.comId(invocacao.getArgument(0), 1));
        var resposta = service.criar("  Ana Silva  ", "  ANA@EXAMPLE.TEST  ", " senha usuario ", " O+ ", "doador");
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Ana Silva");
        assertThat(salvo.getEmail()).isEqualTo("ana@example.test");
        assertThat(salvo.getTipoSanguineo()).isEqualTo("O+");
        assertThat(salvo.getSenha()).startsWith("$2a$");
        assertThat(encoder.matches(" senha usuario ", salvo.getSenha())).isTrue();
        verify(repository).existsByEmailIgnoreCase("ana@example.test");
        assertThat(new ObjectMapper().writeValueAsString(resposta))
                .doesNotContain("senha", "hash", salvo.getSenha());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void doadorExigeTipoSanguineo(String tipo) {
        assertThatThrownBy(() -> service.criar("Ana", "ana@example.test", "segredo", tipo, "DOADOR"))
                .isInstanceOf(TipoSanguineoObrigatorioException.class);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "O+")
    void adminSempreGravaTipoSanguineoNulo(String tipo) {
        when(repository.saveAndFlush(any())).thenAnswer(i -> ServiceFixtures.comId(i.getArgument(0), 1));
        var resposta = service.criar("Admin", "admin@example.test", "segredo", tipo, "ADMIN");
        assertThat(resposta.tipoSanguineo()).isNull();
        assertThat(resposta.perfil()).isEqualTo(PerfilUsuario.ADMIN);
    }

    @Test
    void emailDuplicadoIgnoraMaiusculas() {
        when(repository.existsByEmailIgnoreCase("ana@example.test")).thenReturn(true);
        assertThatThrownBy(() -> service.criar("Ana", " ANA@EXAMPLE.TEST ", "segredo", "O+", "DOADOR"))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ONG", "gerente"})
    void rejeitaPerfilInvalido(String perfil) {
        assertThatThrownBy(() -> service.criar("Ana", "ana@example.test", "segredo", "O+", perfil))
                .isInstanceOf(PerfilInvalidoException.class);
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void nomeEmailESenhaSaoObrigatorios(String valor) {
        assertThatThrownBy(() -> service.criar(valor, "ana@example.test", "segredo", "O+", "DOADOR"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.criar("Ana", valor, "segredo", "O+", "DOADOR"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.criar("Ana", "ana@example.test", valor, "O+", "DOADOR"))
                .isInstanceOf(DadosObrigatoriosException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaSenhaAcimaDe72BytesSemExporSeuConteudo() {
        String senha = "é".repeat(37);
        assertThatThrownBy(() -> service.criar("Ana", "ana@example.test", senha, "O+", "DOADOR"))
                .isInstanceOf(DadosInvalidosException.class).hasMessageNotContaining(senha);
        verifyNoInteractions(repository);
    }

    @Test
    void traduzConflitoDeEmailQueOcorreAposPreConsulta() {
        when(repository.saveAndFlush(any())).thenThrow(ServiceFixtures.constraint("23505", "uk_usuarios_email"));
        assertThatThrownBy(() -> service.criar("Ana", "ana@example.test", "segredo", "O+", "DOADOR"))
                .isInstanceOf(EmailJaCadastradoException.class).hasNoCause();
    }

    @Test
    void naoMascaraOutraFalhaDePersistenciaComoEmailDuplicado() {
        var falha = ServiceFixtures.constraint("23514", "ck_usuarios_perfil");
        when(repository.saveAndFlush(any())).thenThrow(falha);
        assertThatThrownBy(() -> service.criar("Ana", "ana@example.test", "segredo", "O+", "DOADOR"))
                .isSameAs(falha);
    }

    @Test
    void buscaPorEmailNormalizadoEPorIdSemExporSenha() throws Exception {
        Usuario usuario = ServiceFixtures.usuario(1, "Ana");
        when(repository.findByEmailIgnoreCase("ana@example.test")).thenReturn(Optional.of(usuario));
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        var resposta = service.buscarPorEmail(" ANA@EXAMPLE.TEST ");
        assertThat(resposta.id()).isEqualTo(1);
        assertThat(service.buscarPorId(1L)).isEqualTo(resposta);
        assertThat(new ObjectMapper().writeValueAsString(resposta)).doesNotContain("senha", usuario.getSenha());
    }

    @Test
    void buscaInexistenteGeraExcecaoEspecifica() {
        assertThatThrownBy(() -> service.buscarPorId(1L)).isInstanceOf(UsuarioNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscarPorEmail("ausente@example.test")).isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
