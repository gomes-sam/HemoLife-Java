package com.hemolife.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "exames", uniqueConstraints = @UniqueConstraint(
        name = "uk_exames_usuario_data_horario", columnNames = {"usuario_id", "data_exame", "horario"}))
@Check(name = "ck_exames_horario_minuto", constraints = "extract(second from horario) = 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exame {
    private static final DateTimeFormatter FORMATO_HORARIO = DateTimeFormatter.ofPattern("HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exames_usuario"))
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ong_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exames_ong"))
    private Ong ong;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, foreignKey = @ForeignKey(name = "fk_exames_unidade"))
    private Unidade unidade;

    @NotNull
    @Column(name = "data_exame", nullable = false)
    private LocalDate dataExame;

    @NotNull
    @Column(nullable = false)
    private LocalTime horario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusExame status;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /** Referencia opcional ao ObjectId GridFS; nao e uma chave estrangeira SQL. */
    @Size(max = 24)
    @Column(name = "arquivo_id", length = 24)
    private String arquivoId;

    /** O futuro ExameService deve consultar InscricaoRepository antes de agendar. */
    public static Exame agendar(Usuario usuario, Ong ong, Unidade unidade, LocalDate dataExame, LocalTime horario) {
        Exame exame = new Exame();
        exame.usuario = Objects.requireNonNull(usuario, "O usuario deve ser informado.");
        exame.ong = Objects.requireNonNull(ong, "A ONG deve ser informada.");
        exame.unidade = Objects.requireNonNull(unidade, "A unidade deve ser informada.");
        exame.dataExame = Objects.requireNonNull(dataExame, "A data do exame deve ser informada.");
        exame.horario = Objects.requireNonNull(horario, "O horario deve ser informado.");
        exame.status = StatusExame.AGENDADO;
        exame.validarAgendamento();
        return exame;
    }

    /** Aceita estritamente HH:mm, sem segundos e com zero a esquerda. */
    public static Exame agendar(Usuario usuario, Ong ong, Unidade unidade, LocalDate dataExame, String horario) {
        if (horario == null || !horario.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
            throw new IllegalArgumentException("O horario deve utilizar HH:mm.");
        }
        return agendar(usuario, ong, unidade, dataExame, LocalTime.parse(horario, FORMATO_HORARIO));
    }

    /** Cancelamento preserva o registro e e idempotente. */
    public void cancelar() {
        status = StatusExame.CANCELADO;
    }

    public String getHorarioFormatado() {
        return horario.format(FORMATO_HORARIO);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Ong getOng() {
        return ong;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public LocalDate getDataExame() {
        return dataExame;
    }

    public StatusExame getStatus() {
        return status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public String getArquivoId() {
        return arquivoId;
    }

    public void definirArquivoId(String arquivoId) {
        if (arquivoId != null && !arquivoId.matches("[0-9a-fA-F]{24}")) {
            throw new IllegalArgumentException("O arquivoId deve ser um ObjectId de 24 caracteres hexadecimais.");
        }
        this.arquivoId = arquivoId;
    }

    @PrePersist
    private void prepararPersistencia() {
        validarAgendamento();
        criadoEm = LocalDateTime.now();
    }

    private void validarAgendamento() {
        if (dataExame.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data do exame nao pode estar no passado.");
        }
        if (horario.getSecond() != 0 || horario.getNano() != 0) {
            throw new IllegalArgumentException("O horario deve utilizar HH:mm, sem segundos.");
        }
    }
}
