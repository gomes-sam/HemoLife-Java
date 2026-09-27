package com.hemolife.dto;

import com.hemolife.model.Unidade;

public record UnidadeResponse(Long id, String nome, String telefone, String endereco) {
    public static UnidadeResponse de(Unidade unidade) {
        return new UnidadeResponse(unidade.getId(), unidade.getNome(), unidade.getTelefone(), unidade.getEndereco());
    }
}
