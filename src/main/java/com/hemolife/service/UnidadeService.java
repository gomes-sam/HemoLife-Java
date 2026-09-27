package com.hemolife.service;

import com.hemolife.dto.UnidadeResponse;
import com.hemolife.exception.UnidadeNaoEncontradaException;
import com.hemolife.model.Unidade;
import com.hemolife.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnidadeService {
    private final UnidadeRepository unidadeRepository;

    public List<UnidadeResponse> listar() {
        return unidadeRepository.findAllByOrderByNomeAsc().stream().map(UnidadeResponse::de).toList();
    }

    @Transactional
    public UnidadeResponse criar(String nome, String telefone, String endereco) {
        Unidade unidade = new Unidade(ValidacoesNegocio.texto(nome, "nome"),
                ValidacoesNegocio.texto(telefone, "telefone"), ValidacoesNegocio.texto(endereco, "endereco"));
        return UnidadeResponse.de(unidadeRepository.saveAndFlush(unidade));
    }

    public UnidadeResponse buscarPorId(Long id) {
        ValidacoesNegocio.obrigatorio(id, "unidadeId");
        return unidadeRepository.findById(id).map(UnidadeResponse::de).orElseThrow(UnidadeNaoEncontradaException::new);
    }
}
