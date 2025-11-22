package io.github.habatoo.autoconfigurations;

import io.github.habatoo.properties.CorsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.config.CorsRegistration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для проверки корректной загрузки и связывания свойств
 * {@link CorsAutoConfiguration} из конфигурации Spring (application.yaml).
 * <p>
 * Используется {@link ApplicationContextRunner} — облегчённый запуск части контекста
 * без необходимости полностью поднимать приложение. Тест проверяет, что значения
 * CORS-параметров правильно читаются и связываются в бин.
 */
@ActiveProfiles("test")
@DisplayName("Тест загрузки CorsAutoConfiguration")
class CorsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "spring.web.cors.path-pattern=/api/**",
                    "spring.web.cors.allowed-origin-patterns[0]=http://localhost",
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
    static class TestConfig extends CorsAutoConfiguration {
    }

    @Test
    @DisplayName("Автоконфигурация CORS создает бин WebFluxConfigurer и правильно привязывает CorsProperties")
    void testCorsAutoConfigurationCreatesWebMvcConfigurer() {
        contextRunner.run(context -> {
            WebFluxConfigurer configurer = context.getBean(WebFluxConfigurer.class);
            assertThat(configurer).isNotNull();

            CorsProperties corsProps = context.getBean(CorsProperties.class);
            assertThat(corsProps.pathPattern()).isEqualTo("/api/**");
            assertThat(corsProps.allowedOriginPatterns()).contains("http://localhost");
            assertThat(corsProps.allowedMethods()).containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
            assertThat(corsProps.allowedHeaders()).contains("*");
            assertThat(corsProps.allowCredentials()).isTrue();
            assertThat(corsProps.maxAge()).isEqualTo(3600L);
        });
    }

    @Test
    @DisplayName("Конфигурация CorsProperties")
    void testAddCorsMappingsCalledWithExpectedArguments() {
        CorsProperties corsProps = new CorsProperties(
                "/**",
                List.of("http://localhost"),
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"),
                "*",
                true,
                3600L
        );
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);

        when(registry.addMapping(eq("/**"))).thenReturn(registration);
        when(registration.allowedOriginPatterns(eq(new String[]{"http://localhost"}))).thenReturn(registration);
        when(registration.allowedMethods(eq(new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"}))).thenReturn(registration);
        when(registration.allowedHeaders(eq(new String[]{"*"}))).thenReturn(registration);
        when(registration.allowCredentials(eq(true))).thenReturn(registration);
        when(registration.maxAge(eq(3600L))).thenReturn(registration);

        WebFluxConfigurer configurer = new CorsAutoConfiguration().corsWebFluxConfigurer(corsProps);
        configurer.addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedOriginPatterns(new String[]{"http://localhost"});
        verify(registration).allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE", "OPTIONS"});
        verify(registration).allowedHeaders(new String[]{"*"});
        verify(registration).allowCredentials(true);
        verify(registration).maxAge(3600L);
    }
}
