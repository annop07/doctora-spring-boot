package com.example.doctoralia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DatabaseConfigDebug {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigDebug.class);
    
    @Value("${PGHOST:NOT_SET}")
    private String pgHost;
    
    @Value("${PGPORT:NOT_SET}")
    private String pgPort;
    
    @Value("${PGDATABASE:NOT_SET}")
    private String pgDatabase;
    
    @Value("${PGUSER:NOT_SET}")
    private String pgUser;
    
    @Value("${PGPASSWORD:NOT_SET}")
    private String pgPassword;
    
    @Value("${DATABASE_URL:NOT_SET}")
    private String databaseUrl;
    
    @Bean
    CommandLineRunner logDatabaseConfig() {
        return args -> {
            logger.info("=== DATABASE CONFIGURATION DEBUG ===");
            logger.info("PGHOST: {}", pgHost);
            logger.info("PGPORT: {}", pgPort);
            logger.info("PGDATABASE: {}", pgDatabase);
            logger.info("PGUSER: {}", pgUser);
            logger.info("PGPASSWORD: {}", pgPassword != null && !pgPassword.equals("NOT_SET") ? "***SET***" : "NOT_SET");
            logger.info("DATABASE_URL: {}", databaseUrl != null && !databaseUrl.equals("NOT_SET") ? "***SET***" : "NOT_SET");
            logger.info("Constructed URL: jdbc:postgresql://{}:{}/{}", pgHost, pgPort, pgDatabase);
            logger.info("======================================");
        };
    }
}