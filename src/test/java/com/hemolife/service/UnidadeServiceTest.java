package com.hemolife.service;

import com.hemolife.exception.DadosObrigatoriosException;
import com.hemolife.exception.UnidadeNaoEncontradaException;
import com.hemolife.repository.UnidadeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {
    @Mock private UnidadeRepository repository;
    @InjectMocks private UnidadeService service;

    @Test
    void criaUnidadeComStringsNormalizadas() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> ServiceFixtures.comId(i.getArgument(0), 1));
        var resposta = service.criar(" Centro ", " 11999999999 ", " Rua A, 1 ");
        assertThat(resposta.nome()).isEqualTo("Centro");
        assertThat(resposta.telefone()).isEqualTo("11999999999");
        assertThat(resposta.endereco()).isEqualTo("Rua A, 1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void nomeTelefoneEEnderecoSaoObrigatorios(String valor) {
        assertThatThrownBy(() -> service.criar(valor, "11999999999", "Rua A"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.criar("Centro", valor, "Rua A"))
                .isInstanceOf(DadosObrigatoriosException.class);
        assertThatThrownBy(() -> service.criar("Centro", "11999999999", valor))
                .isInstanceOf(DadosObrigatoriosException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void listaOrdenadaPorNome() {
        when(repository.findAllByOrderByNomeAsc()).thenReturn(List.of(
                ServiceFixtures.unidade(1, "Alfa"), ServiceFixtures.unidade(2, "Zulu")));
        assertThat(service.listar()).extracting(r -> r.nome()).containsExactly("Alfa", "Zulu");
        verify(repository).findAllByOrderByNomeAsc();
    }

    @Test
    void buscaPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(ServiceFixtures.unidade(1, "Centro")));
        assertThat(service.buscarPorId(1L).nome()).isEqualTo("Centro");
    }

    @Test
    void unidadeInexistenteTemMensagemDoFlask() {
        assertThatThrownBy(() -> service.buscarPorId(1L))
                .isInstanceOf(UnidadeNaoEncontradaException.class).hasMessage("Unidade nao encontrada.");
    }
}
