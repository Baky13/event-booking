package com.example.eventbooking;

import com.example.eventbooking.config.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
@Import(TestcontainersConfig.class)
class EventBookingApplicationTests {

    @Test
    void contextLoads() {
        // Этот тест проверяет что Spring контекст успешно загружается
        // с реальной PostgreSQL в Testcontainers
    }
}
