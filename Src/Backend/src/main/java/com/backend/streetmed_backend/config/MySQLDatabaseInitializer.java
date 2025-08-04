package com.backend.streetmed_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MySQLDatabaseInitializer {

    private final Logger logger = LoggerFactory.getLogger(MySQLDatabaseInitializer.class);

    @Bean(name = "mysqlDatabaseInitializer") // Changed bean name to avoid conflict
    CommandLineRunner initMySQLDatabase(DataSource dataSource, MongoTemplate mongoTemplate) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            logger.info("Initializing MySQL database connection");

            // Check if MySQL connection is established
            try {
                String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
                logger.info("Successfully connected to MySQL database: {}", dbName);

                // Get MySQL version
                String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
                logger.info("MySQL version: {}", version);

            } catch (Exception e) {
                logger.error("Failed to connect to MySQL database: {}", e.getMessage(), e);
                logger.error("Please check your database credentials and connection settings");
                // Don't rethrow - allow application to start anyway for troubleshooting
            }

            logger.info("MySQL database initialization completed");
        };
    }
}