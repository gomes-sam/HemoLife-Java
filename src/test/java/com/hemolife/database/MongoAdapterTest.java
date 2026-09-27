package com.hemolife.database;

import com.hemolife.exception.DatabaseConnectionException;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoAdapterTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final MongoAdapter adapter = new MongoAdapter(mongoTemplate);

    @Test
    void verificaConexaoComPing() {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenReturn(new Document("ok", 1.0));

        assertThatCode(adapter::verificarConexao).doesNotThrowAnyException();

        verify(mongoTemplate).executeCommand(new Document("ping", 1));
    }

    @Test
    void traduzFalhaMongoPreservandoCausa() {
        var causa = new DataAccessResourceFailureException("Conexao recusada");
        when(mongoTemplate.executeCommand(new Document("ping", 1))).thenThrow(causa);

        assertThatThrownBy(adapter::verificarConexao)
                .isInstanceOf(DatabaseConnectionException.class)
                .hasCause(causa);
    }
}
