package io.github.habatoo.configurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест конфигурации безопасности для модуля.
 */
@DisplayName("Юнит-тесты SecurityConfigurations")
class SecurityConfigurationsTest {

    private final SecurityConfigurations securityConfigurations = new SecurityConfigurations();

    /**
     * Тестирование инициализации бина для цепочки фильтров.
     */
    @Test
    @DisplayName("Проверка успешного создания бина SecurityWebFilterChain")
    void shouldCreateSecurityWebFilterChainTest() {
        ServerHttpSecurity http = Mockito.mock(ServerHttpSecurity.class);

        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeExchange(any())).thenReturn(http);
        when(http.oauth2ResourceServer(any())).thenReturn(http);
        when(http.build()).thenReturn(Mockito.mock(SecurityWebFilterChain.class));

        SecurityWebFilterChain filterChain = securityConfigurations.securityFilterChain(http);

        assertNotNull(filterChain, "Цепочка фильтров не должна быть null");

        verify(http).csrf(any());
        verify(http).authorizeExchange(any());
        verify(http).oauth2ResourceServer(any());
        verify(http).build();
    }
}
