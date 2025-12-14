package io.github.habatoo.services.impl;

import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.BalanceService;
import io.github.habatoo.services.PaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Сервис управления платежами.
 * <p>
 * Предоставляет операции выполнения реактивного платежного действия.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentsServiceImpl implements PaymentsService {

    private final BalanceService balanceService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BalanceResponse> getBalance() {
        return balanceService.getBalance()
                .map(b -> new BalanceResponse().balance(b));
    }

    /**
     * {@inheritDoc}
     */
    public Mono<PaymentResponse> pay(Mono<PaymentRequest> paymentRequest) {
        return paymentRequest.flatMap(req ->
                balanceService.getBalance().flatMap(current -> {
                    var amount = req.getAmount();

                    if (current.compareTo(amount) < 0) {
                        log.info("Недостаточно средств для оплаты {} < {}", current, amount.toString());
                        return Mono.just(new PaymentResponse()
                                .status(PaymentResponse.StatusEnum.FAILED));
                    }

                    log.info("Оплата прошла на сумму {}", amount.toString());
                    return balanceService.decrease(amount)
                            .thenReturn(new PaymentResponse()
                                    .status(PaymentResponse.StatusEnum.SUCCESS));
                })
        );
    }
}
