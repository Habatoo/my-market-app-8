package io.github.habatoo.handler;

import io.github.habatoo.exceptions.PaymentServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Глобальный перехватчик исключений — для централизованной обработки ошибок в контроллерах.
 * Возвращает страницы ошибок с сообщением и кодом.
 */
@Slf4j
@ControllerAdvice
public class PaymentExceptionHandler {

    /**
     * Сервис недоступен — техническая ошибка.
     * HTTP 503.
     */
    @ExceptionHandler(PaymentServiceUnavailableException.class)
    public Mono<ResponseEntity<String>> handleUnavailable(PaymentServiceUnavailableException ex) {
        log.error("Сервис временно недоступен: {}", ex.getMessage());
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Сервис временно недоступен")
        );
    }

    /**
     * Базовый класс для 404, 405 и т.д.), чтобы возвращать статус, заложенный в самой ошибке, а не 500.
     * Логируем как предупреждение, а не как ошибку оплаты
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<String>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Ресурс не найден или недоступен: {}", ex.getReason());
        return Mono.just(ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getReason()));
    }

    /**
     * Любая непредвиденная ошибка.
     * HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<String>> handleGeneric(Exception ex) {
        log.error("Ошбика при оплате: {}", ex.getMessage(), ex);
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Ошбика при оплате")
        );
    }
}
