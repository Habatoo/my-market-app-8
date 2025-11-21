package io.github.habatoo.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Глобальный перехватчик исключений — для централизованной обработки ошибок в контроллерах.
 * Возвращает страницы ошибок с сообщением и кодом.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка невалидных аргументов (валидация входных данных).
     * Возвращает страницу bad-request (400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<String> handleBadRequest(IllegalArgumentException e, Model model) {
        log.warn("Bad request [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        model.addAttribute("error", "Ошибка в параметрах запроса: " + e.getMessage());
        model.addAttribute("status", 400);

        return Mono.just("error/400");
    }

    /**
     * Обработка ошибок взаимодействия с БД (например, DataAccessException).
     * Возвращает страницу с ошибкой БД.
     */
    @ExceptionHandler(DataAccessException.class)
    public Mono<String> handleDatabaseError(Exception e, Model model) {
        log.error("Ошибка базы данных [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        model.addAttribute("error", "Ошибка базы данных (БД): " + e.getMessage());
        model.addAttribute("status", 500);

        return Mono.just("error/db");
    }

    /**
     * Глобальная обработка отсутствия данных при поиске.
     * Возвращает страницу общей ошибки (500).
     */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<String> handleIllegalStateException(IllegalStateException e, Model model) {
        log.error("Некорректное состояние [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        model.addAttribute("error", "Корзина не найдена");
        model.addAttribute("status", 500);

        return Mono.just("error/500");
    }

    /**
     * Глобальная обработка всех прочих (непредвиденных) исключений.
     * Возвращает страницу общей ошибки (500).
     */
    @ExceptionHandler(Exception.class)
    public Mono<String> handleGenericException(Exception e, Model model) {
        log.error("Внутренняя ошибка сервера [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        model.addAttribute("error", "Внутренняя ошибка сервера");
        model.addAttribute("status", 500);

        return Mono.just("error/500");
    }
}
