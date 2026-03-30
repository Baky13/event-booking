package com.example.eventbooking.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Testcontainers
public class TestcontainersConfig {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine")
    ).withDatabaseName("event_booking_test")
     .withUsername("postgres")
     .withPassword("postgres");

    @Bean
    PostgreSQLContainer<?> postgresContainer() {
        return postgres;
    }
}
