package com.hemolife.dto;

import com.hemolife.model.Inscricao;

public record InscricaoResponse(Long id, Long usuarioId, Long ongId) {
    public static InscricaoResponse de(Inscricao inscricao) {
        return new InscricaoResponse(inscricao.getId(), inscricao.getUsuario().getId(), inscricao.getOng().getId());
    }
}
