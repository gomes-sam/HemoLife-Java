package com.hemolife.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unidades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Unidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String nome;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String telefone;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String endereco;

    public Unidade(String nome, String telefone, String endereco) {
        this.nome = nome;
        this.telefone = telefone;
        this.endereco = endereco;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEndereco() {
        return endereco;
    }
}
