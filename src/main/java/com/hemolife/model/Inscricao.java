package com.hemolife.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inscricao", uniqueConstraints = @UniqueConstraint(
        name = "uk_inscricao_usuario_ong", columnNames = {"usuario_id", "ong_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inscricao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inscricao_usuario"))
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ong_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inscricao_ong"))
    private Ong ong;

    public Inscricao(Usuario usuario, Ong ong) {
        this.usuario = usuario;
        this.ong = ong;
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
}
