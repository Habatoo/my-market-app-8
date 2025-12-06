package io.github.habatoo.services.impl;

import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.BalanceService;
import io.github.habatoo.services.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Сервис управления платежами.
 * <p>
 * Предоставляет операции выполнения реактивного платежного действия.
 * </p>
 */
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

                    if (current.compareTo(req.getAmount()) < 0) {
                        return Mono.just(new PaymentResponse()
                                .status(PaymentResponse.StatusEnum.FAILED));
                    }

                    return balanceService.decrease(req.getAmount())
                            .thenReturn(new PaymentResponse()
                                    .status(PaymentResponse.StatusEnum.SUCCESS));
                })
        );
    }
}
