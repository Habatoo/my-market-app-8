package io.github.habatoo.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

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
    public String handleBadRequest(IllegalArgumentException e, Model model) {
        log.warn("Bad request: {}", e.getMessage());
        model.addAttribute("error", "Ошибка в параметрах запроса: " + e.getMessage());
        model.addAttribute("status", 400);
        return "error/400";
    }

    /**
     * Обработка ошибок взаимодействия с БД (например, DataAccessException).
     * Возвращает страницу с ошибкой БД.
     */
    @ExceptionHandler(DataAccessException.class)
    public String handleDatabaseError(Exception e, Model model) {
        log.error("Ошибка базы данных: {}", e.getMessage(), e);
        model.addAttribute("error", "Ошибка базы данных (БД): " + e.getMessage());
        model.addAttribute("status", 500);
        return "error/db";
    }

    /**
     * Обработка ошибки "страница не найдена" (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(Exception e, Model model) {
        log.warn("Страница не найдена: {}", e.getMessage());
        model.addAttribute("error", "Страница не найдена или удалена");
        model.addAttribute("status", 404);
        return "error/404";
    }

    /**
     * Глобальная обработка всех прочих (непредвиденных) исключений.
     * Возвращает страницу общей ошибки (500).
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, Model model) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        model.addAttribute("error", "Внутренняя ошибка сервера");
        model.addAttribute("status", 500);
        return "error/500";
    }
}
