package io.github.habatoo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke-тест, проверяющий, что контекст Spring Boot приложения поднимается без ошибок.
 */
@ActiveProfiles("test")
@SpringBootTest(
        classes = Application.class,
        properties = {
                "spring.config.location=classpath:application-test.yaml"
        }
)
@DisplayName("Проверка загрузки контекста приложения")
class ApplicationBootTest {

    /**
     * Основной smoke-тест: приложение успешно стартует, если тест не падает с ошибкой конфигурации.
     */
    @Test
    @DisplayName("Контекст приложения должен успешно загружаться")
    void contextLoads() {

    }
}
