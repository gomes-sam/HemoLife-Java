package com.hemolife.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email"))
@Check(name = "ck_usuarios_perfil", constraints = "perfil in ('admin', 'doador')")
@Check(name = "ck_usuarios_doador_tipo", constraints = "perfil <> 'doador' or (tipo_sanguineo is not null and length(trim(tipo_sanguineo)) > 0)")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String nome;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String senha;

    @Size(max = 10)
    @Column(name = "tipo_sanguineo", length = 10)
    private String tipoSanguineo;

    @NotNull
    @Convert(converter = PerfilUsuario.Conversor.class)
    @Column(nullable = false, length = 10)
    private PerfilUsuario perfil;

    public Usuario(String nome, String email, String senha, String tipoSanguineo, PerfilUsuario perfil) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.tipoSanguineo = tipoSanguineo;
        this.perfil = perfil;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public String getTipoSanguineo() {
        return tipoSanguineo;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    @AssertTrue(message = "Usuario doador deve possuir tipo sanguineo.")
    public boolean isTipoSanguineoValidoParaPerfil() {
        return perfil != PerfilUsuario.DOADOR || (tipoSanguineo != null && !tipoSanguineo.isBlank());
    }
}
