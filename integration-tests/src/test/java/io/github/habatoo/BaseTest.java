package io.github.habatoo;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Paths;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.security.oauth2.client.registration.test.client-id=test-client",
                "spring.security.oauth2.client.registration.test.client-secret=test-secret",
                "spring.security.oauth2.client.registration.test.authorization-grant-type=authorization_code",
                "spring.security.oauth2.client.registration.test.redirect-uri=http://localhost:8080/login/oauth2/code/keycloak",
                "spring.security.oauth2.client.registration.test.scope=openid,profile",

                "spring.security.oauth2.client.provider.test.authorization-uri=http://localhost:8080/auth",
                "spring.security.oauth2.client.provider.test.token-uri=http://localhost:8080/token",
                "spring.security.oauth2.client.provider.test.jwk-set-uri=http://localhost:8080/jwks",
                "spring.security.oauth2.client.provider.test.user-info-uri=http://localhost:8080/userinfo",

                "spring.r2dbc.pool.enabled=false",
                "spring.r2dbc.pool.validation-query=SELECT 1"
        }
)
@Testcontainers
public abstract class BaseTest {

    static Network network = Network.newNetwork();

    @Container
    static GenericContainer<?> paymentService = new GenericContainer<>(
            new ImageFromDockerfile("payment-test", false)
                    .withDockerfile(Paths.get("../payment/Dockerfile"))
                    .withFileFromPath(".", Paths.get(".."))
                    .withFileFromPath("env", Paths.get("../env"))
    )
            .withNetwork(network)
            .withNetworkAliases("payment-service")
            .withExposedPorts(8081);

    @Container
    static GenericContainer<?> storeService = new GenericContainer<>(
            new ImageFromDockerfile("store-test", false)
                    .withDockerfile(Paths.get("../store/Dockerfile"))
                    .withFileFromPath(".", Paths.get(".."))
                    .withFileFromPath("env", Paths.get("../env"))
    )
            .withNetwork(network)
            .withNetworkAliases("store-service")
            .withEnv("PAYMENT_SERVICE_URL", "http://payment-service:8081")
            .withExposedPorts(8080);

    protected WebTestClient storeClient;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("shop_db")
            .withUsername("test")
            .withPassword("test");

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

    @BeforeEach
    void setUp() {
        String storeUrl = "http://" + storeService.getHost() + ":" + storeService.getMappedPort(8081);
        storeClient = WebTestClient.bindToServer().baseUrl(storeUrl).build();
    }
}

