package io.github.habatoo.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс для проверки корректной загрузки и связывания свойств
 * {@link CorsProperties} из конфигурации Spring (application.yaml).
 * <p>
 * Используется {@link ApplicationContextRunner} — облегчённый запуск части контекста
 * без необходимости полностью поднимать приложение. Тест проверяет, что значения
 * CORS-параметров правильно читаются и связываются в бин.
 */
@ActiveProfiles("test")
@DisplayName("Тест загрузки CorsProperties")
public class CorsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CorsPropertiesTest.TestConfig.class)
            .withPropertyValues(
                    "spring.web.cors.path-pattern=/**",
                    "spring.web.cors.allowed-origin-patterns[0]=http://localhost",
                    "spring.web.cors.allowed-origin-patterns[1]=http://127.0.0.1",
                    "spring.web.cors.allowed-methods[0]=GET",
                    "spring.web.cors.allowed-methods[1]=POST",
                    "spring.web.cors.allowed-methods[2]=PUT",
                    "spring.web.cors.allowed-methods[3]=DELETE",
                    "spring.web.cors.allowed-methods[4]=OPTIONS",
                    "spring.web.cors.allowed-headers=*",
                    "spring.web.cors.allow-credentials=true",
                    "spring.web.cors.max-age=3600"
            );

    @EnableConfigurationProperties(CorsProperties.class)
    static class TestConfig {
    }

    /**
     * Проверяет, что конфигурационные параметры CORS корретно подгружаются
     * и бин {@link CorsProperties} связывается из источника настроек.
     */
    @Test
    @DisplayName("Тест загрузки corsProperties из YAML")
    void shouldLoadPropertiesFromYaml() {
        contextRunner.run(context -> {
            var props = context.getBean(CorsProperties.class);
            assertThat(props.pathPattern()).isEqualTo("/**");
            assertThat(props.allowedOriginPatterns()).contains("http://localhost");
            assertThat(props.allowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
            assertThat(props.allowedHeaders()).contains("*");
            assertThat(props.allowCredentials()).isTrue();
            assertThat(props.maxAge()).isEqualTo(3600L);
        });
    }
}
