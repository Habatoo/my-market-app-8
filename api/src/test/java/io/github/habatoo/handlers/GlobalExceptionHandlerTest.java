package io.github.habatoo.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.ui.Model;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Unit-тесты для GlobalExceptionHandler — проверяют работу перехвата исключений и формирование страниц ошибок.
 */
@ExtendWith(MockitoExtension.class)
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

        String viewName = handler.handleBadRequest(ex, model);

        assertEquals("error/400", viewName);
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

        String viewName = handler.handleDatabaseError(ex, model);

        assertEquals("error/db", viewName);
        verify(model).addAttribute("error", "Ошибка базы данных (БД): БД недоступна");
        verify(model).addAttribute("status", 500);
    }

    /**
     * Тест обработки NoHandlerFoundException (ошибка 404).
     * Проверяет, что возвращается страница ошибки 404 с нужными атрибутами.
     */
    @Test
    @DisplayName("Перехват NoHandlerFoundException — страница 404")
    void testHandleNotFound() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/notfound", new HttpHeaders());

        String viewName = handler.handleNotFound(ex, model);

        assertEquals("error/404", viewName);
        verify(model).addAttribute("error", "Страница не найдена или удалена");
        verify(model).addAttribute("status", 404);
    }

    /**
     * Тест глобального перехвата неизвестных ошибок (Exception).
     * Проверяет возврат страницы 500 с дефолтным сообщением.
     */
    @Test
    @DisplayName("Глобальный перехват Exception — страница 500")
    void testHandleGenericException() {
        Exception ex = new Exception("Критическая ошибка");

        String viewName = handler.handleGenericException(ex, model);

        assertEquals("error/500", viewName);
        verify(model).addAttribute("error", "Внутренняя ошибка сервера");
        verify(model).addAttribute("status", 500);
    }
}
