package com.hemolife.database;

import com.hemolife.exception.DatabaseConnectionException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class PostgresAdapter implements DatabaseAdapter {

    private final DataSource dataSource;

    public PostgresAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void verificarConexao() {
        // O close devolve a conexao ao pool gerenciado pelo Spring.
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new DatabaseConnectionException("Conexao PostgreSQL invalida.");
            }
        } catch (SQLException exception) {
            throw new DatabaseConnectionException("Nao foi possivel acessar o PostgreSQL.", exception);
        }
    }
}
