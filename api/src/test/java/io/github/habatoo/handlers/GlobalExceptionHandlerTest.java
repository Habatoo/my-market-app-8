package io.github.habatoo.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ui.Model;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * Unit-тесты для GlobalExceptionHandler — проверяют работу перехвата исключений и формирование страниц ошибок.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест загрузки GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Mock
    private Model model;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler();
    }

    /**
     * Тест обработки IllegalArgumentException (валидация).
     * Проверяет передачу сообщения и кода ошибки в модель и возврат шаблона ошибки 400.
     */
    @Test
    @DisplayName("Перехват IllegalArgumentException — страница 400")
    void testHandleBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Невалидный параметр");

        Mono<String> viewName = handler.handleBadRequest(ex, model);

        assertNotNull(viewName);
        verify(model).addAttribute("error", "Ошибка в параметрах запроса: Невалидный параметр");
        verify(model).addAttribute("status", 400);
    }

    /**
     * Тест обработки DataAccessException (ошибка работы с БД).
     * Проверяет корректную передачу информации в модель и возврат шаблона ошибки БД.
     */
    @Test
    @DisplayName("Перехват DataAccessException — страница ошибки базы данных")
    void testHandleDatabaseError() {
        DataAccessException ex = new DataAccessResourceFailureException("БД недоступна");
        Mono<String> viewName = handler.handleDatabaseError(ex, model);

        assertNotNull(viewName);
        verify(model).addAttribute("error", "Ошибка базы данных (БД): БД недоступна");
        verify(model).addAttribute("status", 500);
    }

    /**
     * Проверяет обработку IllegalStateException:
     * - устанавливается статус 500
     * - в модель добавляется сообщение "Корзина не найдена" и статус 500
     * - имя view — "error/500"
     */
    @Test
    @DisplayName("Перехват IllegalStateException — страница error/500 и статус 500")
    void testHandleIllegalStateException() {
        IllegalStateException ex = new IllegalStateException("Корзина отсутствует");

        Mono<String> viewName = handler.handleIllegalStateException(ex, model);

        assertNotNull(viewName);
        verify(model).addAttribute("error", "Корзина не найдена");
        verify(model).addAttribute("status", 500);
    }

    /**
     * Тест глобального перехвата неизвестных ошибок (Exception).
     * Проверяет возврат страницы 500 с дефолтным сообщением.
     */
    @Test
    @DisplayName("Глобальный перехват Exception — страница 500")
    void testHandleGenericException() {
        Exception ex = new Exception("Критическая ошибка");

        Mono<String> viewName = handler.handleGenericException(ex, model);

        assertNotNull(viewName);
        verify(model).addAttribute("error", "Внутренняя ошибка сервера");
        verify(model).addAttribute("status", 500);
    }
}
