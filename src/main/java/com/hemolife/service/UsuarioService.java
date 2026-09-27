package com.hemolife.service;

import com.hemolife.dto.UsuarioResponse;
import com.hemolife.exception.EmailJaCadastradoException;
import com.hemolife.exception.PerfilInvalidoException;
import com.hemolife.exception.TipoSanguineoObrigatorioException;
import com.hemolife.exception.UsuarioNaoEncontradoException;
import com.hemolife.model.PerfilUsuario;
import com.hemolife.model.Usuario;
import com.hemolife.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(String nome, String email, String senha, String tipoSanguineo, String perfil) {
        String nomeNormalizado = ValidacoesNegocio.texto(nome, "nome");
        String emailNormalizado = ValidacoesNegocio.email(email);
        String senhaValidada = ValidacoesNegocio.senha(senha, false);
        PerfilUsuario perfilUsuario = interpretarPerfil(perfil);
        String tipo = tipoSanguineo == null ? null : tipoSanguineo.trim();
        if (perfilUsuario == PerfilUsuario.ADMIN) {
            tipo = null;
        } else if (tipo == null || tipo.isBlank()) {
            throw new TipoSanguineoObrigatorioException();
        }
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario(nomeNormalizado, emailNormalizado,
                passwordEncoder.encode(senhaValidada), tipo, perfilUsuario);
        try {
            return UsuarioResponse.de(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            if (ConflitosPersistencia.violou(exception, "23505", "uk_usuarios_email")) {
                throw new EmailJaCadastradoException();
            }
            throw exception;
        }
    }

    public UsuarioResponse buscarPorId(Long id) {
        ValidacoesNegocio.obrigatorio(id, "usuarioId");
        return usuarioRepository.findById(id).map(UsuarioResponse::de)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }

    public UsuarioResponse buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(ValidacoesNegocio.email(email))
                .map(UsuarioResponse::de).orElseThrow(UsuarioNaoEncontradoException::new);
    }

    private PerfilUsuario interpretarPerfil(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            throw new PerfilInvalidoException();
        }
        try {
            return PerfilUsuario.valueOf(perfil.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new PerfilInvalidoException();
        }
    }
}
