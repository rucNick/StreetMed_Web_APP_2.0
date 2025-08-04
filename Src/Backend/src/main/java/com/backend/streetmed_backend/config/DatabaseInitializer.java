package com.backend.streetmed_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseInitializer {

    private final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Bean
    CommandLineRunner initDatabase(DataSource dataSource, MongoTemplate mongoTemplate) {
        return args -> {
            // Initialize MongoDB collections
            if (!mongoTemplate.collectionExists("cargoImages")) {
                mongoTemplate.createCollection("cargoImages");
                logger.info("Created cargoImages collection");
            }

            // Check MySQL connection
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
                logger.info("Connected to MySQL database: {}", dbName);

                // Get database version
                String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
                logger.info("Database version: {}", version);

            } catch (Exception e) {
                logger.error("Database connection error: {}", e.getMessage());
                // Continue startup even with DB error
            }

            logger.info("Database initialization completed!");
        };
    }
}