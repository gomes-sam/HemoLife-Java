package com.hemolife.service;

import com.hemolife.dto.InscricaoResponse;
import com.hemolife.dto.OngResponse;
import com.hemolife.dto.UsuarioResponse;
import com.hemolife.exception.InscricaoDuplicadaException;
import com.hemolife.exception.InscricaoNaoEncontradaException;
import com.hemolife.exception.OngNaoEncontradaException;
import com.hemolife.exception.UsuarioNaoEncontradoException;
import com.hemolife.model.Inscricao;
import com.hemolife.model.Ong;
import com.hemolife.model.Usuario;
import com.hemolife.repository.InscricaoRepository;
import com.hemolife.repository.OngRepository;
import com.hemolife.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InscricaoService {
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OngRepository ongRepository;

    @Transactional
    public InscricaoResponse inscrever(Long usuarioId, Long ongId) {
        validarIds(usuarioId, ongId);
        Ong ong = ongRepository.findById(ongId).orElseThrow(OngNaoEncontradaException::new);
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
        if (inscricaoRepository.existsByUsuarioIdAndOngId(usuarioId, ongId)) {
            throw new InscricaoDuplicadaException();
        }
        try {
            return InscricaoResponse.de(inscricaoRepository.saveAndFlush(new Inscricao(usuario, ong)));
        } catch (DataIntegrityViolationException exception) {
            if (ConflitosPersistencia.violou(exception, "23505", "uk_inscricao_usuario_ong")) {
                throw new InscricaoDuplicadaException();
            }
            throw exception;
        }
    }

    @Transactional
    public void cancelar(Long usuarioId, Long ongId) {
        validarIds(usuarioId, ongId);
        Inscricao inscricao = inscricaoRepository.findByUsuarioIdAndOngId(usuarioId, ongId)
                .orElseThrow(InscricaoNaoEncontradaException::new);
        inscricaoRepository.delete(inscricao);
        inscricaoRepository.flush();
    }

    public List<OngResponse> listarOngsDoUsuario(Long usuarioId) {
        ValidacoesNegocio.obrigatorio(usuarioId, "usuarioId");
        return inscricaoRepository.findByUsuarioId(usuarioId).stream()
                .map(inscricao -> OngResponse.de(inscricao.getOng())).toList();
    }

    public List<UsuarioResponse> listarUsuariosDaOng(Long ongId) {
        ValidacoesNegocio.obrigatorio(ongId, "ongId");
        return inscricaoRepository.findByOngIdOrderByUsuarioNomeAsc(ongId).stream()
                .map(inscricao -> UsuarioResponse.de(inscricao.getUsuario())).toList();
    }

    public boolean jaInscrito(Long usuarioId, Long ongId) {
        validarIds(usuarioId, ongId);
        return inscricaoRepository.existsByUsuarioIdAndOngId(usuarioId, ongId);
    }

    private void validarIds(Long usuarioId, Long ongId) {
        ValidacoesNegocio.obrigatorio(usuarioId, "usuarioId");
        ValidacoesNegocio.obrigatorio(ongId, "ongId");
    }
}
