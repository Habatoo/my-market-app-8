package io.github.habatoo.services.impl;

import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.PaymentsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сервис управления платежами.
 * <p>
 * Предоставляет операции выполнения реактивного платежного действия.
 * </p>
 */
@Service
public class PaymentsServiceImpl implements PaymentsService {

    private final AtomicReference<BigDecimal> balance;

    public PaymentsServiceImpl(@Value("${application.balance}") BigDecimal initialBalance) {
        this.balance = new AtomicReference<>(initialBalance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<BalanceResponse> getBalance() {
        return Mono.fromSupplier(() -> {
            var response = new BalanceResponse();
            response.setBalance(balance.get());

            return response;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<PaymentResponse> pay(Mono<PaymentRequest> paymentRequest) {
        return paymentRequest.map(req -> {
            var current = balance.get();
            if (current.compareTo(req.getAmount()) < 0) {
                return new PaymentResponse().status(PaymentResponse.StatusEnum.FAILED);
            }

            balance.updateAndGet(b -> b.subtract(req.getAmount()));

            return new PaymentResponse().status(PaymentResponse.StatusEnum.SUCCESS);
        });
    }
}
