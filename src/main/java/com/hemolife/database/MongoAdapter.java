package com.hemolife.database;

import com.hemolife.exception.DatabaseConnectionException;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoAdapter implements DatabaseAdapter {

    private final MongoTemplate mongoTemplate;

    public MongoAdapter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void verificarConexao() {
        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
        } catch (DataAccessException exception) {
            throw new DatabaseConnectionException("Nao foi possivel acessar o MongoDB.", exception);
        }
    }
}
