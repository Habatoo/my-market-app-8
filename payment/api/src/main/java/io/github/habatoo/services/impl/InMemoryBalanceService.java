package io.github.habatoo.services.impl;

import io.github.habatoo.services.BalanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сервис работы с балансом пользователя.
 * Определяет операции получения текущего баланса и уменьшения его значения.
 */
@Service
public class InMemoryBalanceService implements BalanceService {

    private final AtomicReference<BigDecimal> balance;

    public InMemoryBalanceService(@Value("${application.balance}") BigDecimal initial) {
        this.balance = new AtomicReference<>(initial);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BigDecimal> getBalance() {
        return Mono.just(balance.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BigDecimal> decrease(BigDecimal amount) {
        return Mono.fromCallable(() -> balance.updateAndGet(b -> b.subtract(amount)));
    }
}

