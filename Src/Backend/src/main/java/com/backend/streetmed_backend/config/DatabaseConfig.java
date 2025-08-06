package com.backend.streetmed_backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Get database connection properties with safe defaults
        String driverClassName = env.getProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        String jdbcUrl = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        // Validate required properties with helpful error messages
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL is required. Please check your application.properties file or set DATABASE_URL environment variable.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Database username is required. Please check your application.properties file or set DB_USERNAME environment variable.");
        }
        if (password == null) {
            throw new IllegalArgumentException("Database password is required. Please check your application.properties file or set DB_PASSWORD environment variable.");
        }

        System.out.println("Connecting to database: " + jdbcUrl);
        System.out.println("Using username: " + username);

        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // Connection pool configuration
        dataSource.setMaximumPoolSize(Integer.parseInt(env.getProperty("spring.datasource.hikari.maximum-pool-size", "10")));
        dataSource.setMinimumIdle(Integer.parseInt(env.getProperty("spring.datasource.hikari.minimum-idle", "5")));
        dataSource.setIdleTimeout(Long.parseLong(env.getProperty("spring.datasource.hikari.idle-timeout", "300000")));
        dataSource.setConnectionTimeout(Long.parseLong(env.getProperty("spring.datasource.hikari.connection-timeout", "20000")));
        dataSource.setMaxLifetime(Long.parseLong(env.getProperty("spring.datasource.hikari.max-lifetime", "1200000")));
        dataSource.setPoolName(env.getProperty("spring.datasource.hikari.pool-name", "MySQLHikariCP"));

        // Check if we're in a Cloud environment (has Cloud SQL specific properties)
        String cloudSqlInstance = env.getProperty("CLOUD_SQL_CONNECTION_NAME");
        boolean isCloudEnvironment = cloudSqlInstance != null && !cloudSqlInstance.trim().isEmpty();

        if (isCloudEnvironment) {
            System.out.println("Configuring for Cloud SQL environment");
            // Cloud SQL specific configurations
            dataSource.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
            dataSource.addDataSourceProperty("cloudSqlInstance", cloudSqlInstance);
        } else {
            System.out.println("Configuring for local MySQL environment");
        }

        // Common MySQL optimizations
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("useLocalSessionState", "true");
        dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
        dataSource.addDataSourceProperty("cacheResultSetMetadata", "true");
        dataSource.addDataSourceProperty("cacheServerConfiguration", "true");
        dataSource.addDataSourceProperty("elideSetAutoCommits", "true");
        dataSource.addDataSourceProperty("maintainTimeStats", "false");

        return dataSource;
    }
}