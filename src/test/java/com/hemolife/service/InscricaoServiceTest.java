package com.hemolife.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemolife.exception.*;
import com.hemolife.model.Inscricao;
import com.hemolife.repository.InscricaoRepository;
import com.hemolife.repository.OngRepository;
import com.hemolife.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {
    @Mock private InscricaoRepository inscricoes;
    @Mock private UsuarioRepository usuarios;
    @Mock private OngRepository ongs;
    @InjectMocks private InscricaoService service;

    @Test
    void verificaExistenciaDaOngPrimeiro() {
        assertThatThrownBy(() -> service.inscrever(1L, 2L)).isInstanceOf(OngNaoEncontradaException.class);
        verifyNoInteractions(usuarios, inscricoes);
    }

    @Test
    void usuarioTambemDeveExistir() {
        when(ongs.findById(2L)).thenReturn(Optional.of(ServiceFixtures.ong(2, "ONG")));
        assertThatThrownBy(() -> service.inscrever(1L, 2L)).isInstanceOf(UsuarioNaoEncontradoException.class);
        verifyNoInteractions(inscricoes);
    }

    @Test
    void rejeitaInscricaoDuplicada() {
        prepararEntidades();
        when(inscricoes.existsByUsuarioIdAndOngId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.inscrever(1L, 2L)).isInstanceOf(InscricaoDuplicadaException.class);
        verify(inscricoes, never()).saveAndFlush(any());
    }

    @Test
    void criaInscricaoEDevolveSomenteIdentificadores() throws Exception {
        prepararEntidades();
        when(inscricoes.saveAndFlush(any())).thenAnswer(i -> ServiceFixtures.comId(i.getArgument(0), 4));
        var resposta = service.inscrever(1L, 2L);
        assertThat(resposta.id()).isEqualTo(4);
        assertThat(resposta.usuarioId()).isEqualTo(1);
        assertThat(resposta.ongId()).isEqualTo(2);
        assertThat(new ObjectMapper().writeValueAsString(resposta)).doesNotContain("senha", "hash-legado");
    }

    @Test
    void traduzConstraintQuandoOutraTransacaoSeInscreveAntes() {
        prepararEntidades();
        when(inscricoes.saveAndFlush(any())).thenThrow(ServiceFixtures.constraint("23505", "uk_inscricao_usuario_ong"));
        assertThatThrownBy(() -> service.inscrever(1L, 2L)).isInstanceOf(InscricaoDuplicadaException.class);
    }

    @Test
    void cancelarExcluiSomenteInscricaoSemReconsultarOngOuUsuario() {
        Inscricao inscricao = ServiceFixtures.inscricao();
        when(inscricoes.findByUsuarioIdAndOngId(1L, 2L)).thenReturn(Optional.of(inscricao));
        service.cancelar(1L, 2L);
        verify(inscricoes).delete(inscricao);
        verify(inscricoes).flush();
        verifyNoInteractions(usuarios, ongs);
    }

    @Test
    void cancelamentoInexistenteTemErroEspecifico() {
        assertThatThrownBy(() -> service.cancelar(1L, 2L)).isInstanceOf(InscricaoNaoEncontradaException.class);
        verify(inscricoes, never()).delete(any());
    }

    @Test
    void listaOngsEMembrosOrdenadosSemExporSenhas() throws Exception {
        var ana = ServiceFixtures.inscricao();
        var zoe = new Inscricao(ServiceFixtures.usuario(5, "Zoe"), ana.getOng());
        when(inscricoes.findByUsuarioId(1L)).thenReturn(List.of(ana));
        when(inscricoes.findByOngIdOrderByUsuarioNomeAsc(2L)).thenReturn(List.of(ana, zoe));
        assertThat(service.listarOngsDoUsuario(1L)).extracting(r -> r.id()).containsExactly(2L);
        assertThat(service.listarUsuariosDaOng(2L)).extracting(r -> r.nome()).containsExactly("Ana", "Zoe");
        var mapper = new ObjectMapper();
        assertThat(mapper.writeValueAsString(service.listarOngsDoUsuario(1L))).doesNotContain("senha", "hash-legado");
        assertThat(mapper.writeValueAsString(service.listarUsuariosDaOng(2L))).doesNotContain("senha", "hash-legado");
    }

    @Test
    void verificaSeUsuarioJaEstaInscrito() {
        when(inscricoes.existsByUsuarioIdAndOngId(1L, 2L)).thenReturn(true);
        assertThat(service.jaInscrito(1L, 2L)).isTrue();
        assertThat(service.jaInscrito(1L, 3L)).isFalse();
    }

    @Test
    void idsSaoObrigatorios() {
        assertThatThrownBy(() -> service.inscrever(null, 2L)).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.inscrever(1L, null)).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.cancelar(null, 2L)).isInstanceOf(DadosObrigatoriosException.class);
        verifyNoInteractions(inscricoes, usuarios, ongs);
    }

    private void prepararEntidades() {
        when(ongs.findById(2L)).thenReturn(Optional.of(ServiceFixtures.ong(2, "ONG")));
        when(usuarios.findById(1L)).thenReturn(Optional.of(ServiceFixtures.usuario(1, "Ana")));
    }
}
