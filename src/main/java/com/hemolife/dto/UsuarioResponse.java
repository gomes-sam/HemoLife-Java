package com.hemolife.dto;

import com.hemolife.model.PerfilUsuario;
import com.hemolife.model.Usuario;

/** Projecao publica sem senha ou hash. */
public record UsuarioResponse(Long id, String nome, String email, String tipoSanguineo, PerfilUsuario perfil) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getTipoSanguineo(), usuario.getPerfil());
    }
}
