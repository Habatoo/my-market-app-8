package io.github.habatoo.services;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Контракт работы с балансом пользователя.
 * Определяет операции получения текущего баланса и уменьшения его значения.
 */
public interface BalanceService {

    /**
     * Возвращает текущий доступный баланс пользователя.
     *
     * @return реактивный publisher, содержащий актуальное значение баланса
     */
    Mono<BigDecimal> getBalance();

    /**
     * Уменьшает текущий баланс на указанную сумму.
     * Предполагается, что вызывающая сторона предварительно проверяет
     * достаточность средств перед вызовом метода.
     *
     * @param amount сумма уменьшения баланса
     * @return реактивный publisher, содержащий новое значение баланса после уменьшения
     */
    Mono<BigDecimal> decrease(BigDecimal amount);
}
