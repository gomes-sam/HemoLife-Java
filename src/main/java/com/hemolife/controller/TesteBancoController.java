package com.hemolife.controller;

import com.hemolife.database.DatabaseManager;
import com.hemolife.dto.TesteBancoResponse;
import com.hemolife.exception.DatabaseConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TesteBancoController {

    private final DatabaseManager databaseManager;

    public TesteBancoController(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @GetMapping(value = "/teste-postgres", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TesteBancoResponse> testarPostgres() {
        return verificarConexao("postgres", "PostgreSQL");
    }

    @GetMapping(value = "/teste-mongo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TesteBancoResponse> testarMongo() {
        return verificarConexao("mongo", "MongoDB");
    }

    private ResponseEntity<TesteBancoResponse> verificarConexao(String nomeAdapter, String banco) {
        try {
            databaseManager.obter(nomeAdapter).verificarConexao();
            return ResponseEntity.ok(new TesteBancoResponse(true, banco, banco + " conectado"));
        } catch (DatabaseConnectionException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TesteBancoResponse(false, banco, "Erro ao conectar ao " + banco));
        }
    }
}
