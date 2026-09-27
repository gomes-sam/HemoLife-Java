package com.hemolife.config;

import com.hemolife.database.DatabaseManager;
import com.hemolife.database.MongoAdapter;
import com.hemolife.database.PostgresAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DatabaseConfig {

    @Bean
    public DatabaseManager databaseManager(PostgresAdapter postgresAdapter, MongoAdapter mongoAdapter) {
        DatabaseManager manager = DatabaseManager.getInstance();
        manager.registrar("postgres", postgresAdapter);
        manager.registrar("mongo", mongoAdapter);
        return manager;
    }
}
