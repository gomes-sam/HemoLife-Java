package com.hemolife.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ong", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ong_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_ong_cnpj", columnNames = "cnpj")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ong {
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

    /** Hash de senha migrado do Flask, sem implementar autenticacao nesta etapa. */
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String senha;

    @NotBlank
    @Size(max = 18)
    @Column(nullable = false, length = 18)
    private String cnpj;

    public Ong(String nome, String email, String senha, String cnpj) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cnpj = cnpj;
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

    public String getCnpj() {
        return cnpj;
    }
}
