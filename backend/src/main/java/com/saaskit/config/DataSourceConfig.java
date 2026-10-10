package com.saaskit.config;

import com.saaskit.tenancy.TenantAwareDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Two DataSources: the raw Hikari pool (used for DDL like CREATE SCHEMA and
 * for Flyway tenant migrations, where the target schema must be explicit)
 * and the primary tenant-aware wrapper that the rest of the application
 * sees. The wrapper points each connection at the current tenant's schema.
 */
@Configuration
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource rawDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource dataSource(HikariDataSource rawDataSource) {
        TenantAwareDataSource wrapper = new TenantAwareDataSource();
        wrapper.setTargetDataSource(rawDataSource);
        return wrapper;
    }
}
