package io.github.habatoo.autoconfigurations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Тестовый класс для проверки конфигурации безопасности SecurityConfigurations.
 * Использует ApplicationContextRunner для проверки регистрации бинов в реактивном контексте.
 */
@DisplayName("Тест конфигурации SecurityConfigurations")
class SecurityConfigurationsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityConfigurations.class))
            .withBean(
                    ReactiveClientRegistrationRepository.class,
                    () -> mock(ReactiveClientRegistrationRepository.class)
            );

    @Test
    @DisplayName("Проверка создания бинов SecurityWebFilterChain и JwtConverter")
    void testSecurityBeansRegistration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
            assertThat(context).hasSingleBean(ReactiveJwtAuthenticationConverter.class);
        });
    }

    @Test
    @DisplayName("Проверка логики ReactiveJwtAuthenticationConverter (Юнит-тест бина)")
    void testJwtAuthenticationConverterLogic() {
        contextRunner.run(context -> {
            ReactiveJwtAuthenticationConverter converter = context.getBean(ReactiveJwtAuthenticationConverter.class);

            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .claim("realm_access.roles", List.of("ADMIN", "MANAGER"))
                    .build();

            StepVerifier.create(converter.convert(jwt))
                    .assertNext(auth -> {
                        List<String> authorities = auth.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .toList();

                        assertThat(authorities)
                                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MANAGER");
                    })
                    .verifyComplete();
        });
    }

    @Test
    @DisplayName("Проверка наличия LogoutWebFilter в цепочке")
    void testLogoutFilterIsPresent() {
        contextRunner.run(context -> {
            SecurityWebFilterChain chain = context.getBean(SecurityWebFilterChain.class);

            boolean hasLogoutFilter = Boolean.TRUE.equals(chain.getWebFilters()
                    .collectList()
                    .map(filters -> filters.stream().anyMatch(f -> f instanceof LogoutWebFilter))
                    .block());

            assertThat(hasLogoutFilter).isTrue();
        });
    }

    @Test
    @DisplayName("Проверка правил доступа (Security Matchers)")
    void testSecurityMatchers() {
        contextRunner.run(context -> {
            SecurityWebFilterChain chain = context.getBean(SecurityWebFilterChain.class);

            ServerWebExchange mockExchange = mock(ServerWebExchange.class);
            StepVerifier.create(chain.matches(mockExchange))
                    .expectNext(true)
                    .verifyComplete();
        });
    }
}
