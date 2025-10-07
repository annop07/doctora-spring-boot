package com.example.doctoralia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

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
        String jdbcUrl;
        String finalUsername;
        String finalPassword;

        System.out.println("=== DATABASE CONFIGURATION DEBUG ===");
        System.out.println("DATABASE_URL: " + (databaseUrl != null && !databaseUrl.isEmpty() ? databaseUrl : "[NOT SET]"));
        System.out.println("PGHOST: " + host);
        System.out.println("PGPORT: " + port);
        System.out.println("PGDATABASE: " + database);
        System.out.println("PGUSER: " + username);
        System.out.println("PGPASSWORD: " + (password != null && !password.equals("password") ? "[SET]" : "[NOT SET]"));

        // If DATABASE_URL is provided, parse it (Railway format: postgresql://user:pass@host:port/db)
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            try {
                URI uri = new URI(databaseUrl);
                
                // Extract components from URI
                String hostName = uri.getHost();
                int portNumber = uri.getPort() != -1 ? uri.getPort() : 5432;
                String databaseName = uri.getPath().substring(1); // Remove leading '/'
                
                // Build JDBC URL without user info in the URL
                jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", hostName, portNumber, databaseName);
                
                // Extract username and password from userInfo
                if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                    String[] userInfoParts = uri.getUserInfo().split(":", 2);
                    finalUsername = userInfoParts[0];
                    finalPassword = userInfoParts[1];
                } else {
                    finalUsername = username;
                    finalPassword = password;
                }
                
                System.out.println("Using DATABASE_URL");
            } catch (Exception e) {
                System.out.println("Failed to parse DATABASE_URL, falling back to individual variables: " + e.getMessage());
                jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
                finalUsername = username;
                finalPassword = password;
            }
        } else {
            // Use individual PG variables
            jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
            finalUsername = username;
            finalPassword = password;
            System.out.println("Using individual PG variables");
        }
        
        System.out.println("Final JDBC URL: " + jdbcUrl);
        System.out.println("Final Username: " + finalUsername);
        System.out.println("Final Password: " + (finalPassword != null && !finalPassword.equals("password") ? "[SET]" : "[NOT SET]"));
        System.out.println("=====================================");

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(jdbcUrl)
                .username(finalUsername)
                .password(finalPassword)
                .build();
    }
}