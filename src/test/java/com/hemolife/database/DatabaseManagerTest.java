package com.hemolife.database;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseManagerTest {

    @Test
    void compartilhaUmaUnicaInstanciaEntreThreads() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            Callable<DatabaseManager> obterInstancia = DatabaseManager::getInstance;
            for (var resultado : executor.invokeAll(Collections.nCopies(16, obterInstancia))) {
                assertThat(resultado.get()).isSameAs(DatabaseManager.getInstance());
            }
        }
    }

    @Test
    void permiteRegistrarObterESubstituirAdapterPeloNome() {
        DatabaseManager manager = DatabaseManager.getInstance();
        String nome = UUID.randomUUID().toString();
        DatabaseAdapter inicial = () -> { };
        DatabaseAdapter substituto = () -> { };

        manager.registrar(nome, inicial);
        assertThat(DatabaseManager.getInstance().obter(nome)).isSameAs(inicial);

        manager.registrar(nome, substituto);
        assertThat(manager.obter(nome)).isSameAs(substituto);
    }

    @Test
    void informaQuandoAdapterNaoFoiRegistrado() {
        String nome = UUID.randomUUID().toString();
        assertThatThrownBy(() -> DatabaseManager.getInstance().obter(nome))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(nome);
    }

    @Test
    void rejeitaRegistrosInvalidos() {
        DatabaseManager manager = DatabaseManager.getInstance();
        assertThatThrownBy(() -> manager.registrar(" ", () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.obter(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.registrar("invalido", null))
                .isInstanceOf(NullPointerException.class);
    }
}
