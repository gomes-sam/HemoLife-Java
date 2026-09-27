package com.hemolife.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemolife.exception.*;
import com.hemolife.model.Ong;
import com.hemolife.repository.OngRepository;
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
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OngServiceTest {
    @Mock private OngRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private OngService service;

    @BeforeEach
    void preparar() {
        service = new OngService(repository, encoder);
    }

    @Test
    void cadastroNormalizaCamposEUsaBCryptSemRetornarSenha() throws Exception {
        when(repository.saveAndFlush(any())).thenAnswer(i -> ServiceFixtures.comId(i.getArgument(0), 1));
        var resposta = service.cadastrar("  ONG  ", " ONG@EXAMPLE.TEST ", " segredo ong ", " 12345678000199 ");
        var captor = ArgumentCaptor.forClass(Ong.class);
        verify(repository).saveAndFlush(captor.capture());
        Ong ong = captor.getValue();
        assertThat(ong.getNome()).isEqualTo("ONG");
        assertThat(ong.getEmail()).isEqualTo("ong@example.test");
        assertThat(ong.getCnpj()).isEqualTo("12345678000199");
        assertThat(encoder.matches("segredo ong", ong.getSenha())).isTrue();
        assertThat(new ObjectMapper().writeValueAsString(resposta)).doesNotContain("senha", ong.getSenha(), "segredo ong");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void exigeTodosOsCampos(String valor) {
        assertThatThrownBy(() -> service.cadastrar(valor, "ong@example.test", "segredo", "12345678000199"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.cadastrar("ONG", valor, "segredo", "12345678000199"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.cadastrar("ONG", "ong@example.test", valor, "12345678000199"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.cadastrar("ONG", "ong@example.test", "segredo", valor))
                .isInstanceOf(DadosObrigatoriosException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejeitaEmailDuplicado() {
        when(repository.existsByEmailIgnoreCase("ong@example.test")).thenReturn(true);
        assertThatThrownBy(() -> service.cadastrar("ONG", " ONG@EXAMPLE.TEST ", "segredo", "12345678000199"))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejeitaCnpjDuplicado() {
        when(repository.existsByCnpj("12345678000199")).thenReturn(true);
        assertThatThrownBy(() -> service.cadastrar("ONG", "ong@example.test", "segredo", " 12345678000199 "))
                .isInstanceOf(CnpjJaCadastradoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void atualizacaoIgnoraProprioIdEPreservaSenhaAoAlterarSomenteTresCampos() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.atualizarDados(1L, "Novo nome", "ong@example.test", "12345678000199")).thenReturn(1);
        var resposta = service.atualizar(1L, " Novo nome ", " ONG@EXAMPLE.TEST ", " 12345678000199 ");
        assertThat(resposta.nome()).isEqualTo("Novo nome");
        verify(repository).existsByEmailIgnoreCaseAndIdNot("ong@example.test", 1L);
        verify(repository).existsByCnpjAndIdNot("12345678000199", 1L);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void atualizacaoRejeitaEmailDeOutraOng() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsByEmailIgnoreCaseAndIdNot("outra@example.test", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.atualizar(1L, "ONG", " OUTRA@EXAMPLE.TEST ", "12345678000199"))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).atualizarDados(any(), any(), any(), any());
    }

    @Test
    void atualizacaoRejeitaCnpjDeOutraOng() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsByCnpjAndIdNot("12345678000199", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.atualizar(1L, "ONG", "ong@example.test", "12345678000199"))
                .isInstanceOf(CnpjJaCadastradoException.class);
        verify(repository, never()).atualizarDados(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_ong_email", "uk_ong_cnpj"})
    void trataConflitoConcorrenteNoCadastro(String constraint) {
        when(repository.saveAndFlush(any())).thenThrow(ServiceFixtures.constraint("23505", constraint));
        Class<? extends NegocioException> tipo = constraint.equals("uk_ong_email")
                ? EmailJaCadastradoException.class : CnpjJaCadastradoException.class;
        assertThatThrownBy(() -> service.cadastrar("ONG", "ong@example.test", "segredo", "12345678000199"))
                .isInstanceOf(tipo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"uk_ong_email", "uk_ong_cnpj"})
    void trataConflitoConcorrenteNaAtualizacao(String constraint) {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.atualizarDados(any(), any(), any(), any())).thenThrow(ServiceFixtures.constraint("23505", constraint));
        Class<? extends NegocioException> tipo = constraint.equals("uk_ong_email")
                ? EmailJaCadastradoException.class : CnpjJaCadastradoException.class;
        assertThatThrownBy(() -> service.atualizar(1L, "ONG", "ong@example.test", "12345678000199"))
                .isInstanceOf(tipo);
    }

    @Test
    void listaOrdenadaEBuscaIdComRespostaSemSenha() throws Exception {
        var alfa = ServiceFixtures.ong(1, "Alfa");
        var zulu = ServiceFixtures.ong(2, "Zulu");
        when(repository.findAllByOrderByNomeAsc()).thenReturn(List.of(alfa, zulu));
        when(repository.findById(1L)).thenReturn(Optional.of(alfa));
        assertThat(service.listar()).extracting(r -> r.nome()).containsExactly("Alfa", "Zulu");
        assertThat(service.buscarPorId(1L).nome()).isEqualTo("Alfa");
        assertThat(new ObjectMapper().writeValueAsString(service.listar())).doesNotContain("senha", "hash-legado");
    }

    @Test
    void ongInexistenteNaoPodeSerBuscadaAtualizadaOuDeletada() {
        assertThatThrownBy(() -> service.buscarPorId(1L)).isInstanceOf(OngNaoEncontradaException.class);
        assertThatThrownBy(() -> service.atualizar(1L, "ONG", "ong@example.test", "12345678000199"))
                .isInstanceOf(OngNaoEncontradaException.class);
        assertThatThrownBy(() -> service.deletar(1L)).isInstanceOf(OngNaoEncontradaException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void detectaRemocaoEntreConsultaEAtualizacao() {
        when(repository.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.atualizar(1L, "ONG", "ong@example.test", "12345678000199"))
                .isInstanceOf(OngNaoEncontradaException.class);
    }

    @Test
    void deletaOngExistente() {
        when(repository.excluirPorId(1L)).thenReturn(1);
        service.deletar(1L);
        verify(repository).excluirPorId(1L);
    }

    @Test
    void informaQuandoOngAindaTemVinculos() {
        when(repository.excluirPorId(1L)).thenThrow(ServiceFixtures.constraint("23503", "fk_exames_ong"));
        assertThatThrownBy(() -> service.deletar(1L)).isInstanceOf(OngEmUsoException.class);
    }
}
