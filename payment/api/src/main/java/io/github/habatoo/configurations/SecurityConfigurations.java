package io.github.habatoo.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Конфигурация безопасности для WebFlux приложения.
 * <p>
 * Отключает CSRF, требует аутентификацию для всех запросов
 * и настраивает сервер ресурсов OAuth2 с поддержкой JWT.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfigurations {

    /**
     * Настройка цепочки фильтров безопасности.
     * * @param http строитель безопасности Spring Security.
     * @return настроенная цепочка фильтров.
     */
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
                .oauth2ResourceServer(spec -> spec.jwt(Customizer.withDefaults()))
                .build();
    }
}
