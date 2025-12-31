package io.github.habatoo.services.impl;

import io.github.habatoo.storages.RedisBalanceStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.TestSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты RedisBalanceService")
class RedisBalanceServiceTest {

    private final BigDecimal initialBalance = new BigDecimal("300.00");

    private final String testUsername = "user-123";

    @Mock
    private RedisBalanceStorage redisBalanceStorage;

    private RedisBalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new RedisBalanceService(initialBalance, redisBalanceStorage);
    }

    @Test
    @DisplayName("getBalance: возврат баланса из хранилища, если он там есть")
    void getBalanceFromStorageTest() {
        mockJwtContext(testUsername);
        BigDecimal storedBalance = new BigDecimal("150.00");
        when(redisBalanceStorage.getBalanceByName(anyString())).thenReturn(Mono.just(storedBalance));
        var securityContext = TestSecurityContextHolder.getContext();

        StepVerifier.create(
                        balanceService.getBalance()
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                        Mono.just(securityContext)))
                )
                .expectNextMatches(val -> val.compareTo(storedBalance) == 0)
                .verifyComplete();

        verify(redisBalanceStorage).getBalanceByName(testUsername);
    }

    @Test
    @DisplayName("getBalance: возврат начального баланса, если в Redis пусто")
    void getBalanceEmptyInRedisReturnsInitialTest() {
        mockJwtContext(testUsername);
        when(redisBalanceStorage.getBalanceByName(testUsername)).thenReturn(Mono.empty());
        var securityContext = TestSecurityContextHolder.getContext();

        StepVerifier.create(
                        balanceService.getBalance()
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                        Mono.just(securityContext)))
                )
                .expectNext(initialBalance)
                .verifyComplete();
    }

    @Test
    @DisplayName("decrease: успешное уменьшение баланса")
    void decreaseSuccessTest() {
        mockJwtContext(testUsername);
        BigDecimal currentBalance = new BigDecimal("300.00");
        BigDecimal amount = new BigDecimal("50.00");
        BigDecimal expectedBalance = new BigDecimal("250.00");
        var securityContext = TestSecurityContextHolder.getContext();

        when(redisBalanceStorage.getBalanceByName(testUsername)).thenReturn(Mono.just(currentBalance));
        when(redisBalanceStorage.saveBalanceByName(testUsername, expectedBalance)).thenReturn(Mono.empty());

        StepVerifier.create(balanceService.decrease(amount)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(securityContext)))
                )
                .expectNext(expectedBalance)
                .verifyComplete();

        verify(redisBalanceStorage).saveBalanceByName(testUsername, expectedBalance);
    }

    @Test
    @DisplayName("reset: сохранение новой суммы и возврат Void")
    void resetSuccessTest() {
        mockJwtContext(testUsername);
        BigDecimal newAmount = new BigDecimal("500.00");
        var securityContext = TestSecurityContextHolder.getContext();
        when(redisBalanceStorage.saveBalanceByName(testUsername, newAmount)).thenReturn(Mono.empty());

        StepVerifier.create(balanceService.reset(newAmount)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(securityContext)))
                )
                .verifyComplete();

        verify(redisBalanceStorage).saveBalanceByName(testUsername, newAmount);
    }

    @Test
    @DisplayName("getCurrentUsername: проверка работы с обычным Authentication (не JWT)")
    void getCurrentUsernameSimpleAuthTest() {
        mockJwtContext("regular-user");
        when(redisBalanceStorage.getBalanceByName("regular-user")).thenReturn(Mono.empty());
        var securityContext = TestSecurityContextHolder.getContext();
        StepVerifier.create(balanceService.getBalance()

                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(securityContext)))
                )
                .expectNext(initialBalance)
                .verifyComplete();
    }

    @Test
    @DisplayName("Сброс баланса на отрицательное значение — ошибка")
    void testResetNegativeBalanceTest() {
        StepVerifier.create(balanceService.reset(BigDecimal.valueOf(-100)))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Баланс не может быть отрицательным"))
                .verify();
    }

    /**
     * Вспомогательный метод для имитации JWT авторизации в реактивном контексте.
     */
    private void mockJwtContext(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", subject)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        SecurityContext context = new SecurityContextImpl(auth);
        TestSecurityContextHolder.setContext(context);
    }
}
