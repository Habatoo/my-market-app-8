package io.github.habatoo.handler;

import io.github.habatoo.exceptions.PaymentServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для PaymentExceptionHandler — проверяют работу перехвата исключений.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест PaymentExceptionHandler")
class PaymentExceptionHandlerTest {

    private PaymentExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new PaymentExceptionHandler();
    }

    /**
     * Тест глобального перехвата ошибок платежного сервиса (PaymentServiceUnavailableException).
     * Проверяет возврат ошибки с дефолтным сообщением.
     */
    @Test
    @DisplayName("Перехват PaymentServiceUnavailableException — возвращает 503 и сообщение")
    void testHandleUnavailable() {
        PaymentServiceUnavailableException ex = new PaymentServiceUnavailableException();

        Mono<ResponseEntity<String>> result = handler.handleUnavailable(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody()).isEqualTo("Сервис временно недоступен");
                })
                .verifyComplete();
    }

    /**
     * Тест глобального перехвата неизвестных ошибок (Exception).
     * Проверяет возврат ошибки с дефолтным сообщением.
     */
    @Test
    @DisplayName("Глобальный перехват Exception — возвращает 500 и сообщение")
    void testHandleGenericException() {
        Exception ex = new Exception("Критическая ошибка");

        Mono<ResponseEntity<String>> result = handler.handleGeneric(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isEqualTo("Ошбика при оплате");
                })
                .verifyComplete();
    }
}
