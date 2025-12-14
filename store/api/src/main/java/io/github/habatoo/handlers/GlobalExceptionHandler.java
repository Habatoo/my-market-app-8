package io.github.habatoo.handlers;

import io.github.habatoo.exceptions.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

/**
 * Глобальный перехватчик исключений — для централизованной обработки ошибок в контроллерах.
 * Возвращает страницы ошибок с сообщением и кодом.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 — ошибка валидации или неверные параметры запроса.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<String> handleBadRequest(IllegalArgumentException e, Model model) {
        log.warn("Bad request: {}", e.getMessage());
        return renderError(model, 400,
                "Ошибка в параметрах запроса: " + e.getMessage(),
                "error/400");
    }

    /**
     * 404 — сущность или ресурс не найден.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public Mono<String> handleNotFound(NoSuchElementException e, Model model) {
        log.warn("Resource not found: {}", e.getMessage());
        return renderError(model, 404,
                "Страница не найдена или удалена",
                "error/404");
    }

    /**
     * 500 — ошибка работы с базой данных.
     */
    @ExceptionHandler(DataAccessException.class)
    public Mono<String> handleDatabaseError(DataAccessException e, Model model) {
        log.error("Database error: {}", e.getMessage(), e);
        return renderError(model, 500,
                "Ошибка базы данных (БД): " + e.getMessage(),
                "error/db");
    }

    /**
     * 400 — ошибки взаимодействия с внешним сервисом платежей.
     */
    @ExceptionHandler(PaymentException.class)
    public Mono<String> handlePaymentError(PaymentException e, Model model) {
        log.error("Payment service error: {}", e.getMessage(), e);
        return renderError(model, 400,
                "Ошибка обращения к сервису платежей",
                "error/400");
    }

    /**
     * 500 — ошибочное состояние (например, некорректный доменный сценарий).
     */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<String> handleIllegalStateException(IllegalStateException e, Model model) {
        log.error("Illegal state: {}", e.getMessage(), e);

        String message = (e.getMessage() == null || e.getMessage().isBlank())
                ? "Внутренняя ошибка сервера"
                : e.getMessage();

        return renderError(model, 500, message, "error/500");
    }

    /**
     * 500 — обработка всех непредвиденных ошибок.
     */
    @ExceptionHandler(Exception.class)
    public Mono<String> handleGenericException(Exception e, Model model) {
        log.error("Unexpected server error: {}", e.getMessage(), e);
        return renderError(model, 500,
                "Внутренняя ошибка сервера",
                "error/500");
    }

    private Mono<String> renderError(Model model, int status, String message, String viewName) {
        return Mono.defer(() -> {
            model.addAttribute("status", status);
            model.addAttribute("error", message);
            return Mono.just(viewName);
        });
    }
}
