package com.hemolife.service;

import com.hemolife.dto.ExameResponse;
import com.hemolife.exception.ConflitoAgendamentoException;
import com.hemolife.exception.DataExameInvalidaException;
import com.hemolife.exception.ExameNaoEncontradoException;
import com.hemolife.exception.HorarioInvalidoException;
import com.hemolife.exception.OngNaoEncontradaException;
import com.hemolife.exception.UnidadeNaoEncontradaException;
import com.hemolife.exception.UsuarioNaoEncontradoException;
import com.hemolife.exception.UsuarioNaoInscritoException;
import com.hemolife.model.Exame;
import com.hemolife.model.Ong;
import com.hemolife.model.Unidade;
import com.hemolife.model.Usuario;
import com.hemolife.repository.ExameRepository;
import com.hemolife.repository.InscricaoRepository;
import com.hemolife.repository.OngRepository;
import com.hemolife.repository.UnidadeRepository;
import com.hemolife.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExameService {
    private final ExameRepository exameRepository;
    private final UsuarioRepository usuarioRepository;
    private final OngRepository ongRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UnidadeRepository unidadeRepository;

    @Transactional
    public ExameResponse agendar(Long usuarioId, Long ongId, Long unidadeId, LocalDate dataExame, String horario) {
        ValidacoesNegocio.obrigatorio(usuarioId, "usuarioId");
        ValidacoesNegocio.obrigatorio(ongId, "ongId");
        ValidacoesNegocio.obrigatorio(unidadeId, "unidadeId");
        ValidacoesNegocio.obrigatorio(dataExame, "dataExame");
        ValidacoesNegocio.texto(horario, "horario");

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
        Ong ong = ongRepository.findById(ongId).orElseThrow(OngNaoEncontradaException::new);
        if (!inscricaoRepository.existsByUsuarioIdAndOngId(usuarioId, ongId)) {
            throw new UsuarioNaoInscritoException();
        }
        Unidade unidade = unidadeRepository.findById(unidadeId).orElseThrow(UnidadeNaoEncontradaException::new);
        if (dataExame.isBefore(LocalDate.now())) {
            throw new DataExameInvalidaException();
        }
        if (!horario.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
            throw new HorarioInvalidoException();
        }
        LocalTime hora = LocalTime.parse(horario);
        if (exameRepository.existsByUsuarioIdAndDataExameAndHorario(usuarioId, dataExame, hora)) {
            throw new ConflitoAgendamentoException();
        }
        Exame exame = Exame.agendar(usuario, ong, unidade, dataExame, hora);
        try {
            return ExameResponse.de(exameRepository.saveAndFlush(exame));
        } catch (DataIntegrityViolationException exception) {
            if (ConflitosPersistencia.violou(exception, "23505", "uk_exames_usuario_data_horario")) {
                throw new ConflitoAgendamentoException();
            }
            throw exception;
        }
    }

    public List<ExameResponse> listarDoUsuario(Long usuarioId) {
        ValidacoesNegocio.obrigatorio(usuarioId, "usuarioId");
        return exameRepository.findByUsuarioIdOrderByDataExameAscHorarioAsc(usuarioId).stream()
                .map(ExameResponse::de).toList();
    }

    @Transactional
    public ExameResponse cancelar(Long usuarioId, Long exameId) {
        ValidacoesNegocio.obrigatorio(usuarioId, "usuarioId");
        ValidacoesNegocio.obrigatorio(exameId, "exameId");
        Exame exame = exameRepository.findByIdAndUsuarioId(exameId, usuarioId)
                .orElseThrow(ExameNaoEncontradoException::new);
        exame.cancelar();
        return ExameResponse.de(exameRepository.saveAndFlush(exame));
    }
}
