package com.hemolife.controller;

import com.hemolife.config.DatabaseConfig;
import com.hemolife.config.SecurityConfig;
import com.hemolife.database.DatabaseManager;
import com.hemolife.database.MongoAdapter;
import com.hemolife.database.PostgresAdapter;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TesteBancoController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, DatabaseConfig.class, PostgresAdapter.class, MongoAdapter.class})
class TesteBancoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseManager databaseManager;

    @Autowired
    private PostgresAdapter postgresAdapter;

    @Autowired
    private MongoAdapter mongoAdapter;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Test
    void verificaPostgresPeloSingletonSemAutenticacao() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);

        assertThat(databaseManager).isSameAs(DatabaseManager.getInstance());
        assertThat(databaseManager.obter("postgres")).isSameAs(postgresAdapter);

        mockMvc.perform(get("/api/teste-postgres"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(
                        "{\"success\":true,\"banco\":\"PostgreSQL\",\"mensagem\":\"PostgreSQL conectado\"}"));

        verify(connection).isValid(5);
        verify(connection).close();
        verify(mongoTemplate, never()).executeCommand(any(Document.class));
    }

    @Test
    void retorna500QuandoPostgresFalhaSemExporDetalhesInternos() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Detalhe interno do driver JDBC"));

        mockMvc.perform(get("/api/teste-postgres"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(
                        "{\"success\":false,\"banco\":\"PostgreSQL\",\"mensagem\":\"Erro ao conectar ao PostgreSQL\"}"));

        verify(dataSource).getConnection();
        verify(mongoTemplate, never()).executeCommand(any(Document.class));
    }

    @Test
    void verificaMongoPeloSingletonSemAutenticacao() throws Exception {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenReturn(new Document("ok", 1.0));

        assertThat(databaseManager).isSameAs(DatabaseManager.getInstance());
        assertThat(databaseManager.obter("mongo")).isSameAs(mongoAdapter);

        mockMvc.perform(get("/api/teste-mongo"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(
                        "{\"success\":true,\"banco\":\"MongoDB\",\"mensagem\":\"MongoDB conectado\"}"));

        verify(mongoTemplate).executeCommand(new Document("ping", 1));
        verifyNoInteractions(dataSource);
    }

    @Test
    void retorna500QuandoMongoFalhaSemExporDetalhesInternos() throws Exception {
        when(mongoTemplate.executeCommand(new Document("ping", 1)))
                .thenThrow(new DataAccessResourceFailureException("Detalhe interno do driver MongoDB"));

        mockMvc.perform(get("/api/teste-mongo"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(
                        "{\"success\":false,\"banco\":\"MongoDB\",\"mensagem\":\"Erro ao conectar ao MongoDB\"}"));

        verify(mongoTemplate).executeCommand(new Document("ping", 1));
        verifyNoInteractions(dataSource);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/teste-postgres", "/api/teste-mongo"})
    @WithMockUser
    void bloqueiaPostMesmoComUsuarioECsrfValido(String rota) throws Exception {
        mockMvc.perform(post(rota).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dataSource);
        verify(mongoTemplate, never()).executeCommand(any(Document.class));
    }
}
