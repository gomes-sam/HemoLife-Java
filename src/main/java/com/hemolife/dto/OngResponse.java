package com.hemolife.dto;

import com.hemolife.model.Ong;

/** Projecao publica sem senha ou hash. */
public record OngResponse(Long id, String nome, String email, String cnpj) {
    public static OngResponse de(Ong ong) {
        return new OngResponse(ong.getId(), ong.getNome(), ong.getEmail(), ong.getCnpj());
    }
}
