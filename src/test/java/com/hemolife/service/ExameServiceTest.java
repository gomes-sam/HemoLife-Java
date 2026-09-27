package com.hemolife.service;

import com.hemolife.exception.*;
import com.hemolife.model.PerfilUsuario;
import com.hemolife.model.StatusExame;
import com.hemolife.model.Usuario;
import com.hemolife.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExameServiceTest {
    @Mock private ExameRepository exames;
    @Mock private UsuarioRepository usuarios;
    @Mock private OngRepository ongs;
    @Mock private InscricaoRepository inscricoes;
    @Mock private UnidadeRepository unidades;
    @InjectMocks private ExameService service;

    @Test
    void validaTodosOsObrigatoriosAntesDeConsultarRepositorios() {
        LocalDate data = LocalDate.now();
        assertThatThrownBy(() -> service.agendar(null, 2L, 3L, data, "09:00")).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.agendar(1L, null, 3L, data, "09:00")).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.agendar(1L, 2L, null, data, "09:00")).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, null, "09:00")).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, data, null)).isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, data, " ")).isInstanceOf(DadosObrigatoriosException.class);
        verifyNoInteractions(exames, usuarios, ongs, inscricoes, unidades);
    }

    @Test
    void usuarioDeveExistir() {
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00"))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
        verifyNoInteractions(ongs, inscricoes, unidades, exames);
    }

    @Test
    void ongInexistenteTemMensagemDoFlask() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(ServiceFixtures.usuario(1, "Ana")));
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00"))
                .isInstanceOf(OngNaoEncontradaException.class).hasMessage("ONG nao encontrada.");
        verifyNoInteractions(inscricoes, unidades, exames);
    }

    @Test
    void exigeInscricaoAntesDeBuscarUnidade() {
        prepararUsuarioEOng();
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00"))
                .isInstanceOf(UsuarioNaoInscritoException.class);
        verifyNoInteractions(unidades, exames);
    }

    @Test
    void unidadeInexistenteTemMensagemDoFlask() {
        prepararUsuarioEOng();
        when(inscricoes.existsByUsuarioIdAndOngId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00"))
                .isInstanceOf(UnidadeNaoEncontradaException.class).hasMessage("Unidade nao encontrada.");
        verifyNoInteractions(exames);
    }

    @Test
    void rejeitaDataPassada() {
        prepararAgendamento();
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now().minusDays(1), "09:00"))
                .isInstanceOf(DataExameInvalidaException.class);
        verifyNoInteractions(exames);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9:00", "09:00:00", "24:00", "12:60", " 09:00", "09:00 "})
    void horarioDeveSerEstritamenteHHmm(String horario) {
        prepararAgendamento();
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), horario))
                .isInstanceOf(HorarioInvalidoException.class);
        verifyNoInteractions(exames);
    }

    @Test
    void agendaCorretamenteNaSequenciaObrigatoria() {
        prepararAgendamento();
        when(exames.saveAndFlush(any())).thenAnswer(i -> ServiceFixtures.comId(i.getArgument(0), 4));
        LocalDate data = LocalDate.now().plusDays(1);
        var resposta = service.agendar(1L, 2L, 3L, data, "09:00");
        assertThat(resposta.usuarioId()).isEqualTo(1);
        assertThat(resposta.ongId()).isEqualTo(2);
        assertThat(resposta.unidadeId()).isEqualTo(3);
        assertThat(resposta.dataExame()).isEqualTo(data);
        assertThat(resposta.horario()).isEqualTo("09:00");
        assertThat(resposta.status()).isEqualTo(StatusExame.AGENDADO);
        var ordem = inOrder(usuarios, ongs, inscricoes, unidades, exames);
        ordem.verify(usuarios).findById(1L);
        ordem.verify(ongs).findById(2L);
        ordem.verify(inscricoes).existsByUsuarioIdAndOngId(1L, 2L);
        ordem.verify(unidades).findById(3L);
        ordem.verify(exames).existsByUsuarioIdAndDataExameAndHorario(1L, data, LocalTime.of(9, 0));
        ordem.verify(exames).saveAndFlush(any());
    }

    @Test
    void usuarioValidoSignificaExistenteSemAcrescentarRestricaoDePerfil() {
        Usuario admin = ServiceFixtures.comId(new Usuario("Admin", "admin@example.test", "hash", null, PerfilUsuario.ADMIN), 1);
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        when(ongs.findById(2L)).thenReturn(Optional.of(ServiceFixtures.ong(2, "ONG")));
        when(inscricoes.existsByUsuarioIdAndOngId(1L, 2L)).thenReturn(true);
        when(unidades.findById(3L)).thenReturn(Optional.of(ServiceFixtures.unidade(3, "Centro")));
        when(exames.saveAndFlush(any())).thenAnswer(i -> ServiceFixtures.comId(i.getArgument(0), 4));
        assertThat(service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00").status()).isEqualTo(StatusExame.AGENDADO);
    }

    @Test
    void impedeMesmoUsuarioDataHorarioIndependentementeDeOngEUnidade() {
        prepararAgendamento();
        when(exames.existsByUsuarioIdAndDataExameAndHorario(1L, LocalDate.now(), LocalTime.of(9, 0))).thenReturn(true);
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00"))
                .isInstanceOf(ConflitoAgendamentoException.class);
        verify(exames, never()).saveAndFlush(any());
    }

    @Test
    void traduzUniqueQuandoOutraTransacaoReservaHorario() {
        prepararAgendamento();
        when(exames.saveAndFlush(any())).thenThrow(ServiceFixtures.constraint("23505", "uk_exames_usuario_data_horario"));
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00"))
                .isInstanceOf(ConflitoAgendamentoException.class).hasNoCause();
    }

    @Test
    void naoConverteErroDeFkEmConflitoDeHorario() {
        prepararAgendamento();
        var falha = ServiceFixtures.constraint("23503", "fk_exames_unidade");
        when(exames.saveAndFlush(any())).thenThrow(falha);
        assertThatThrownBy(() -> service.agendar(1L, 2L, 3L, LocalDate.now(), "09:00")).isSameAs(falha);
    }

    @Test
    void listaNaOrdemDataEHorario() {
        LocalDate hoje = LocalDate.now();
        when(exames.findByUsuarioIdOrderByDataExameAscHorarioAsc(1L)).thenReturn(List.of(
                ServiceFixtures.exame(4, hoje, "09:00"), ServiceFixtures.exame(5, hoje, "14:00"),
                ServiceFixtures.exame(6, hoje.plusDays(1), "08:00")));
        assertThat(service.listarDoUsuario(1L)).extracting(r -> r.id()).containsExactly(4L, 5L, 6L);
    }

    @Test
    void cancelaHistoricoSemConsultarInscricaoENuncaExclui() {
        var exame = ServiceFixtures.exame(4, LocalDate.now(), "09:00");
        ReflectionTestUtils.setField(exame, "dataExame", LocalDate.now().minusDays(1));
        when(exames.findByIdAndUsuarioId(4L, 1L)).thenReturn(Optional.of(exame));
        when(exames.saveAndFlush(exame)).thenReturn(exame);
        var resposta = service.cancelar(1L, 4L);
        assertThat(resposta.status()).isEqualTo(StatusExame.CANCELADO);
        assertThat(exame.getStatus()).isEqualTo(StatusExame.CANCELADO);
        verify(exames, never()).delete(any());
        verifyNoInteractions(inscricoes, usuarios, ongs, unidades);
    }

    @Test
    void exameDeOutroUsuarioETratadoComoNaoEncontrado() {
        assertThatThrownBy(() -> service.cancelar(99L, 4L))
                .isInstanceOf(ExameNaoEncontradoException.class).hasMessage("Exame nao encontrado.");
        verify(exames).findByIdAndUsuarioId(4L, 99L);
        verify(exames, never()).findById(any());
        verify(exames, never()).saveAndFlush(any());
        verifyNoInteractions(inscricoes, usuarios, ongs, unidades);
    }

    private void prepararUsuarioEOng() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(ServiceFixtures.usuario(1, "Ana")));
        when(ongs.findById(2L)).thenReturn(Optional.of(ServiceFixtures.ong(2, "ONG")));
    }

    private void prepararAgendamento() {
        prepararUsuarioEOng();
        when(inscricoes.existsByUsuarioIdAndOngId(1L, 2L)).thenReturn(true);
        when(unidades.findById(3L)).thenReturn(Optional.of(ServiceFixtures.unidade(3, "Centro")));
    }
}
