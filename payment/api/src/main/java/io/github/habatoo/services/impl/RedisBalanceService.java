package io.github.habatoo.services.impl;

import io.github.habatoo.services.BalanceService;
import io.github.habatoo.storages.RedisBalanceStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Сервис работы с балансом пользователя.
 * Определяет операции получения текущего баланса и уменьшения его значения.
 */
@Service
@Slf4j
public class RedisBalanceService implements BalanceService {

    private final BigDecimal initialBalance;
    private final RedisBalanceStorage redisBalanceStorage;

    public RedisBalanceService(
            @Value("${application.balance:300.00}") BigDecimal initialBalance,
            RedisBalanceStorage redisBalanceStorage) {
        this.initialBalance = initialBalance;
        this.redisBalanceStorage = redisBalanceStorage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BigDecimal> getBalance() {
        return getCurrentUsername()
                .flatMap(username -> redisBalanceStorage.getBalanceByName(username)
                        .doOnNext(val -> log.info("Загружен баланс из Redis для {}: {}", username, val))
                        .switchIfEmpty(Mono.just(initialBalance)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BigDecimal> decrease(BigDecimal amount) {
        return getCurrentUsername()
                .flatMap(username -> getBalance()
                        .doOnNext(u -> log.info("Оплата для {} на сумму {}", u, amount))
                        .flatMap(current -> {
                            BigDecimal newBalance = current.subtract(amount);
                            return redisBalanceStorage.saveBalanceByName(username, newBalance)
                                    .thenReturn(newBalance);
                        }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> reset(BigDecimal amount) {
        if (amount.signum() < 0) {
            return Mono.error(new IllegalArgumentException("Баланс не может быть отрицательным"));
        }

        return getCurrentUsername()
                .flatMap(username -> redisBalanceStorage.saveBalanceByName(username, amount))
                .then();
    }

    private Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    if (auth.getPrincipal() instanceof Jwt jwt) {
                        log.warn("Успешно прочитан токен для externalId {}", jwt.getSubject());
                        return jwt.getSubject();
                    }
                    return auth.getName();
                });
    }
}
