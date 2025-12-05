package io.github.habatoo.services;

import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import reactor.core.publisher.Mono;

/**
 * Контракт управления платежами.
 * <p>
 * Предоставляет операции выполнения реактивного платежного действия и
 * операции получения текущего состояния баланса.
 * </p>
 */
public interface PaymentsService {

    /**
     * Возвращает текущий баланс пользователя.
     * <p>
     * Реализация данного метода должна быть не блокирующей и
     * предоставлять актуальный баланс на момент вызова.
     * </p>
     *
     * @return реактивный publisher с информацией о балансе
     */
    Mono<BalanceResponse> getBalance();

    /**
     * Выполняет списание указанной суммы баланса.
     * <p>
     * Выполнение платежа приводит к уменьшению баланса,
     * если средств достаточно. Если средств недостаточно,
     * возвращается ответ со статусом FAILED.
     * </p>
     *
     * @param paymentRequest реактивный publisher данных платежа
     * @return реактивный publisher с результатом обработки платежа
     */
    Mono<PaymentResponse> pay(Mono<PaymentRequest> paymentRequest);
}
