package com.hemolife.service;

import com.hemolife.dto.OngResponse;
import com.hemolife.exception.CnpjJaCadastradoException;
import com.hemolife.exception.EmailJaCadastradoException;
import com.hemolife.exception.OngEmUsoException;
import com.hemolife.exception.OngNaoEncontradaException;
import com.hemolife.model.Ong;
import com.hemolife.repository.OngRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OngService {

    private final OngRepository ongRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public OngResponse cadastrar(
            String nome,
            String email,
            String senha,
            String cnpj
    ) {
        String nomeNormalizado =
                ValidacoesNegocio.texto(nome, "nome");

        String emailNormalizado =
                ValidacoesNegocio.email(email);

        String senhaNormalizada =
                ValidacoesNegocio.senha(senha, true);

        String cnpjNormalizado =
                ValidacoesNegocio.texto(cnpj, "cnpj");

        if (ongRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new EmailJaCadastradoException();
        }

        if (ongRepository.existsByCnpj(cnpjNormalizado)) {
            throw new CnpjJaCadastradoException();
        }

        Ong ong = new Ong(
                nomeNormalizado,
                emailNormalizado,
                passwordEncoder.encode(senhaNormalizada),
                cnpjNormalizado
        );

        try {
            return OngResponse.de(
                    ongRepository.saveAndFlush(ong)
            );
        } catch (DataIntegrityViolationException exception) {
            throw traduzirConflito(exception);
        }
    }

    public OngResponse autenticar(
            String email,
            String senha
    ) {
        String emailNormalizado =
                ValidacoesNegocio.email(email);

        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException(
                    "Credenciais invalidas."
            );
        }

        Ong ong = ongRepository
                .findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Credenciais invalidas."
                        )
                );

        if (!passwordEncoder.matches(
                senha,
                ong.getSenha()
        )) {
            throw new IllegalArgumentException(
                    "Credenciais invalidas."
            );
        }

        return OngResponse.de(ong);
    }

    public List<OngResponse> listar() {
        return ongRepository
                .findAllByOrderByNomeAsc()
                .stream()
                .map(OngResponse::de)
                .toList();
    }

    public OngResponse buscarPorId(Long id) {
        ValidacoesNegocio.obrigatorio(
                id,
                "ongId"
        );

        return ongRepository
                .findById(id)
                .map(OngResponse::de)
                .orElseThrow(
                        OngNaoEncontradaException::new
                );
    }

    @Transactional
    public OngResponse atualizar(
            Long id,
            String nome,
            String email,
            String cnpj
    ) {
        ValidacoesNegocio.obrigatorio(
                id,
                "ongId"
        );

        if (!ongRepository.existsById(id)) {
            throw new OngNaoEncontradaException();
        }

        String nomeNormalizado =
                ValidacoesNegocio.texto(
                        nome,
                        "nome"
                );

        String emailNormalizado =
                ValidacoesNegocio.email(email);

        String cnpjNormalizado =
                ValidacoesNegocio.texto(
                        cnpj,
                        "cnpj"
                );

        if (ongRepository
                .existsByEmailIgnoreCaseAndIdNot(
                        emailNormalizado,
                        id
                )) {
            throw new EmailJaCadastradoException();
        }

        if (ongRepository
                .existsByCnpjAndIdNot(
                        cnpjNormalizado,
                        id
                )) {
            throw new CnpjJaCadastradoException();
        }

        try {

            int atualizados =
                    ongRepository.atualizarDados(
                            id,
                            nomeNormalizado,
                            emailNormalizado,
                            cnpjNormalizado
                    );

            if (atualizados == 0) {
                throw new OngNaoEncontradaException();
            }

            return new OngResponse(
                    id,
                    nomeNormalizado,
                    emailNormalizado,
                    cnpjNormalizado
            );

        } catch (DataIntegrityViolationException exception) {
            throw traduzirConflito(exception);
        }
    }

    @Transactional
    public void deletar(Long id) {
        ValidacoesNegocio.obrigatorio(
                id,
                "ongId"
        );

        try {

            int removidos =
                    ongRepository.excluirPorId(id);

            if (removidos == 0) {
                throw new OngNaoEncontradaException();
            }

        } catch (DataIntegrityViolationException exception) {

            if (ConflitosPersistencia.violou(
                    exception,
                    "23503",
                    "fk_inscricao_ong",
                    "fk_exames_ong"
            )) {
                throw new OngEmUsoException();
            }

            throw exception;
        }
    }

    private RuntimeException traduzirConflito(
            DataIntegrityViolationException exception
    ) {
        if (ConflitosPersistencia.violou(
                exception,
                "23505",
                "uk_ong_email"
        )) {
            return new EmailJaCadastradoException();
        }

        if (ConflitosPersistencia.violou(
                exception,
                "23505",
                "uk_ong_cnpj"
        )) {
            return new CnpjJaCadastradoException();
        }

        return exception;
    }
}