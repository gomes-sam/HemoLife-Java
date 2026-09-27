package com.hemolife.config;

import com.hemolife.database.DatabaseManager;
import com.hemolife.database.MongoAdapter;
import com.hemolife.database.PostgresAdapter;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DatabaseConfigTest {

    @Test
    void registraAdaptersNoSingletonSemAbrirConexoes() {
        DataSource dataSource = mock(DataSource.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);

        new ApplicationContextRunner()
                .withUserConfiguration(DatabaseConfig.class, PostgresAdapter.class, MongoAdapter.class)
                .withBean(DataSource.class, () -> dataSource)
                .withBean(MongoTemplate.class, () -> mongoTemplate)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DatabaseManager manager = context.getBean(DatabaseManager.class);
                    assertThat(manager).isSameAs(DatabaseManager.getInstance());
                    assertThat(manager.obter("postgres")).isSameAs(context.getBean(PostgresAdapter.class));
                    assertThat(manager.obter("mongo")).isSameAs(context.getBean(MongoAdapter.class));
                    verifyNoInteractions(dataSource);
                    verify(mongoTemplate, never()).executeCommand(any(Document.class));
                });
    }
}
