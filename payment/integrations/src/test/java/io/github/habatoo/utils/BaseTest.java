package io.github.habatoo.utils;

import io.github.habatoo.Application;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Collections;

@SpringBootTest(classes = Application.class)
public abstract class BaseTest {

    @Container
    @ServiceConnection
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideOAuth2Properties(DynamicPropertyRegistry registry) {
        String kReg = "spring.security.oauth2.client.registration.keycloak";
        String kProv = "spring.security.oauth2.client.provider.keycloak";

        registry.add(kReg + ".client-id", () -> "test-client");
        registry.add(kReg + ".client-secret", () -> "test-secret");
        registry.add(kReg + ".authorization-grant-type", () -> "authorization_code");
        registry.add(kReg + ".redirect-uri", () -> "{baseUrl}/login/oauth2/code/{registrationId}");

        registry.add(kProv + ".authorization-uri", () -> "http://localhost:9999/auth");
        registry.add(kProv + ".token-uri", () -> "http://localhost:9999/token");
        registry.add(kProv + ".jwk-set-uri", () -> "http://localhost:9999/jwks");

        String tReg = "spring.security.oauth2.client.registration.test";
        String tProv = "spring.security.oauth2.client.provider.test";

        registry.add(tReg + ".client-id", () -> "test-client");
        registry.add(tReg + ".client-secret", () -> "test-secret");
        registry.add(tReg + ".authorization-grant-type", () -> "authorization_code");
        registry.add(tReg + ".redirect-uri", () -> "{baseUrl}/login/oauth2/code/{registrationId}");

        registry.add(tProv + ".authorization-uri", () -> "http://localhost:9999/auth");
        registry.add(tProv + ".token-uri", () -> "http://localhost:9999/token");
        registry.add(tProv + ".jwk-set-uri", () -> "http://localhost:9999/jwks");
    }

    /**
     * Вспомогательный метод для имитации JWT контекста
     */
    protected Context createJwtContext(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", subject)
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setAuthenticated(true);

        return Context.of(SecurityContext.class, Mono.just(new SecurityContextImpl(auth)));
    }
}
