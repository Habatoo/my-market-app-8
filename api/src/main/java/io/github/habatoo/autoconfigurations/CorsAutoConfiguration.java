package io.github.habatoo.autoconfigurations;

import io.github.habatoo.properties.CorsProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Автоматическая конфигурация CORS для доступа к localhost.
 * Не требует явного @Configuration и @EnableConfigurationProperties в основном приложении.
 */
@AutoConfiguration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsAutoConfiguration {

    @Bean
    public WebMvcConfigurer corsWebMvcConfigurer(CorsProperties corsProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping(corsProperties.pathPattern())
                        .allowedOriginPatterns(corsProperties.allowedOriginPatterns().toArray(new String[0]))
                        .allowedMethods(corsProperties.allowedMethods().toArray(new String[0]))
                        .allowedHeaders(corsProperties.allowedHeaders())
                        .allowCredentials(corsProperties.allowCredentials())
                        .maxAge(corsProperties.maxAge());
            }
        };
    }
}
