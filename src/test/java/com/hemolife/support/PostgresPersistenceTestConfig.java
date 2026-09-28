package com.hemolife.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.IOException;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresPersistenceTestConfig {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
                .setPort(0)
                .setServerConfig("listen_addresses", "localhost")
                .start();
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres postgres) {
        return postgres.getPostgresDatabase();
    }
}
