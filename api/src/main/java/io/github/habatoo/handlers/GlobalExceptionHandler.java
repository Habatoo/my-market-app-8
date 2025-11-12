package io.github.habatoo.handlers;

import jakarta.servlet.http.HttpServletResponse;
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
    public String handleBadRequest(IllegalArgumentException e, Model model, HttpServletResponse response) {
        log.warn("Bad request [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("error", "Ошибка в параметрах запроса: " + e.getMessage());
        model.addAttribute("status", 400);

        return "error/400";
    }

    /**
     * Обработка ошибок взаимодействия с БД (например, DataAccessException).
     * Возвращает страницу с ошибкой БД.
     */
    @ExceptionHandler(DataAccessException.class)
    public String handleDatabaseError(Exception e, Model model, HttpServletResponse response) {
        log.error("Ошибка базы данных [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("error", "Ошибка базы данных (БД): " + e.getMessage());
        model.addAttribute("status", 500);

        return "error/db";
    }

    /**
     * Обработка ошибки "страница не найдена" (404).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound(Exception e, Model model, HttpServletResponse response) {
        log.warn("Страница не найдена [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("error", "Страница не найдена или удалена");
        model.addAttribute("status", 404);

        return "error/404";
    }

    /**
     * Глобальная обработка отсутствия данных при поиске.
     * Возвращает страницу общей ошибки (500).
     */
    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException e, Model model, HttpServletResponse response) {
        log.error("Некорректное состояние [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("error", "Корзина не найдена");
        model.addAttribute("status", 500);

        return "error/500";
    }

    /**
     * Глобальная обработка всех прочих (непредвиденных) исключений.
     * Возвращает страницу общей ошибки (500).
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception e, Model model, HttpServletResponse response) {
        log.error("Внутренняя ошибка сервера [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("error", "Внутренняя ошибка сервера");
        model.addAttribute("status", 500);

        return "error/500";
    }
}

