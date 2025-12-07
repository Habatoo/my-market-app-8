package io.github.habatoo.controllers;

import io.github.habatoo.payment.api.PaymentsApi;
import io.github.habatoo.payment.model.BalanceResponse;
import io.github.habatoo.payment.model.PaymentRequest;
import io.github.habatoo.payment.model.PaymentResponse;
import io.github.habatoo.services.PaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * REST-контроллер сервиса платежей.
 * <p>
 * Реализует API создания платежей и управления балансом кошелька
 * на основе автоматически сгенерированного интерфейса PaymentsApi.
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentsController implements PaymentsApi {

    private final PaymentsService paymentsService;

    /**
     * Создаёт новый платеж и списывает сумму с текущего баланса пользователя.
     * <p>
     * При успешном выполнении операция возвращает статус SUCCESS и HTTP-код 201.
     * При недостатке средств возвращается статус FAILED.
     * </p>
     *
     * @param contentType    MIME-тип тела запроса.
     * @param paymentRequest входящие данные платежа (сумма списания)
     * @param exchange       информация о текущем HTTP-запросе
     * @return реактивный publisher с HTTP-ответом и результатом выполнения платежа
     */
    @Override
    public Mono<ResponseEntity<PaymentResponse>> createPayment(
            String contentType,
            Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {
        return paymentRequest
                .doOnNext(p -> log.info("POST /payments/payment — request={}", p))
                .transform(paymentsService::pay)
                .map(body -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                );
    }

    /**
     * Возвращает текущий баланс кошелька пользователя.
     * <p>
     * Метод реализует HTTP-операцию получения баланса
     * и возвращает экземпляр {@link BalanceResponse}
     * с доступной суммой средств.
     * </p>
     *
     * @param exchange информация о текущем запросе, включая HTTP-метаданные
     * @return реактивный publisher с HTTP-ответом, содержащим информацию о балансе
     */
    @Override
    public Mono<ResponseEntity<BalanceResponse>> getWalletBalance(
            ServerWebExchange exchange) {
        log.info("GET /payments/balance — запрос баланса");

        return paymentsService
                .getBalance()
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }
}
