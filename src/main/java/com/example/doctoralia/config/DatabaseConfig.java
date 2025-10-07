package com.example.doctoralia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${PGHOST:localhost}")
    private String host;

    @Value("${PGPORT:5432}")
    private String port;

    @Value("${PGDATABASE:railway}")
    private String database;

    @Value("${PGUSER:postgres}")
    private String username;

    @Value("${PGPASSWORD:password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
        
        System.out.println("=== DATABASE CONFIGURATION DEBUG ===");
        System.out.println("PGHOST: " + host);
        System.out.println("PGPORT: " + port);
        System.out.println("PGDATABASE: " + database);
        System.out.println("PGUSER: " + username);
        System.out.println("PGPASSWORD: " + (password != null ? "[SET]" : "[NOT SET]"));
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("=====================================");

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }
}