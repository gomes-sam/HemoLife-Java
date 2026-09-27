package com.hemolife.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExameDomainTest {
    private final Usuario usuario = new Usuario("Doador", "doador@example.test", "hash-legado", "O+", PerfilUsuario.DOADOR);
    private final Ong ong = new Ong("ONG", "ong@example.test", "hash-legado", "12345678000199");
    private final Unidade unidade = new Unidade("Unidade", "11999999999", "Rua de teste, 1");

    @Test
    void agendamentoUsaUsuarioOngEUnidadeInformados() {
        Exame exame = Exame.agendar(usuario, ong, unidade, LocalDate.now(), "09:05");
        assertThat(exame.getUsuario()).isSameAs(usuario);
        assertThat(exame.getOng()).isSameAs(ong);
        assertThat(exame.getUnidade()).isSameAs(unidade);
        assertThat(exame.getHorario()).isEqualTo(LocalTime.of(9, 5));
        assertThat(exame.getHorarioFormatado()).isEqualTo("09:05");
        assertThat(exame.getStatus()).isEqualTo(StatusExame.AGENDADO);
        assertThat(exame.getArquivoId()).isNull();
    }

    @Test
    void rejeitaDataPassada() {
        assertThatThrownBy(() -> Exame.agendar(usuario, ong, unidade, LocalDate.now().minusDays(1), "09:00"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("passado");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"9:00", "09:00:00", "24:00", "12:60", " 09:00", "09:00 "})
    void aceitaSomenteHorarioHHmm(String horario) {
        assertThatThrownBy(() -> Exame.agendar(usuario, ong, unidade, LocalDate.now(), horario))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("HH:mm");
    }

    @Test
    void rejeitaSegundosMesmoComLocalTime() {
        assertThatThrownBy(() -> Exame.agendar(usuario, ong, unidade, LocalDate.now(), LocalTime.of(9, 0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaFracoesDeSegundo() {
        assertThatThrownBy(() -> Exame.agendar(usuario, ong, unidade, LocalDate.now(), LocalTime.of(9, 0, 0, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeUsuarioOngEUnidade() {
        assertThatThrownBy(() -> Exame.agendar(null, ong, unidade, LocalDate.now(), "09:00"))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("usuario");
        assertThatThrownBy(() -> Exame.agendar(usuario, null, unidade, LocalDate.now(), "09:00"))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("ONG");
        assertThatThrownBy(() -> Exame.agendar(usuario, ong, null, LocalDate.now(), "09:00"))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("unidade");
    }

    @Test
    void cancelarEIdempotenteEPreservaAgendamento() {
        Exame exame = Exame.agendar(usuario, ong, unidade, LocalDate.now(), "09:00");
        exame.cancelar();
        exame.cancelar();
        assertThat(exame.getStatus()).isEqualTo(StatusExame.CANCELADO);
        assertThat(exame.getUsuario()).isSameAs(usuario);
        assertThat(exame.getDataExame()).isEqualTo(LocalDate.now());
    }

    @Test
    void arquivoIdEReferenciaOpcionalSemUpload() {
        Exame exame = Exame.agendar(usuario, ong, unidade, LocalDate.now(), "09:00");
        exame.definirArquivoId("507f1f77bcf86cd799439011");
        assertThat(exame.getArquivoId()).isEqualTo("507f1f77bcf86cd799439011");
        exame.definirArquivoId(null);
        assertThat(exame.getArquivoId()).isNull();
        assertThatThrownBy(() -> exame.definirArquivoId("id-invalido"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
