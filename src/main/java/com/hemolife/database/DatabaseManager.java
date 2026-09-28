package com.hemolife.database;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public final class DatabaseManager {

    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final Map<String, DatabaseAdapter> adapters = new ConcurrentHashMap<>();

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }


    public void registrar(String nome, DatabaseAdapter adapter) {
        validarNome(nome);
        adapters.put(nome, Objects.requireNonNull(adapter, "O adapter nao pode ser nulo."));
    }

    public DatabaseAdapter obter(String nome) {
        validarNome(nome);
        DatabaseAdapter adapter = adapters.get(nome);
        if (adapter == null) {
            throw new NoSuchElementException("Adapter nao registrado: " + nome);
        }
        return adapter;
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do adapter deve ser informado.");
        }
    }
}
