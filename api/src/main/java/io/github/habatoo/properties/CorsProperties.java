package io.github.habatoo.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Класс для биндинга настроек из файла конфигурации.
 * <p>
 * Связывает свойства с префиксом "spring.web.cors" из application.yml
 * Содержит параметры для настройки паттерна, разрешённых источников, HTTP-методов, заголовков,
 * параметра allowCredentials и maxAge для CORS.
 * <p>
 */
@ConfigurationProperties(prefix = "spring.web.cors")
public record CorsProperties(
        String pathPattern,
        List<String> allowedOriginPatterns,
        List<String> allowedMethods,
        String allowedHeaders,
        boolean allowCredentials,
        Long maxAge
) {
}
