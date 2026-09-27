package com.hemolife.database;

import com.hemolife.exception.DatabaseConnectionException;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostgresAdapterTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PostgresAdapter adapter = new PostgresAdapter(dataSource);

    @Test
    void verificaConexaoEDevolveAoPool() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        assertThatCode(adapter::verificarConexao).doesNotThrowAnyException();

        verify(connection).close();
    }

    @Test
    void sinalizaConexaoInvalidaEDevolveAoPool() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);

        assertThatThrownBy(adapter::verificarConexao)
                .isInstanceOf(DatabaseConnectionException.class);

        verify(connection).close();
    }

    @Test
    void traduzFalhaSqlPreservandoCausa() throws SQLException {
        SQLException causa = new SQLException("Conexao recusada");
        when(dataSource.getConnection()).thenThrow(causa);

        assertThatThrownBy(adapter::verificarConexao)
                .isInstanceOf(DatabaseConnectionException.class)
                .hasCause(causa);
    }
}
