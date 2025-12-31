package io.github.habatoo.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

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
     *
     * @return настроенная цепочка фильтров.
     */
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/payments/**").hasRole("USER")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(spec -> spec
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            return Mono.justOrEmpty(jwt.getClaimAsMap("realm_access"))
                    .flatMapMany(realmAccess -> {
                        Object roles = realmAccess.get("roles");

                        if (roles instanceof Collection<?> rolesList) {
                            return Flux.fromIterable(rolesList)
                                    .filter(String.class::isInstance)
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role));
                        }

                        return Flux.empty();
                    });
        });

        return converter;
    }
}
