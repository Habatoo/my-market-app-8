package io.github.habatoo.handlers;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

/**
 * Тестовый REST контроллер, демонстрирующий выбрасываемые исключения.
 *
 * <p>Содержит методы, которые намеренно выбрасывают разные исключения,
 * чтобы проверить глобальную обработку ошибок и поведение приложения при ошибках.</p>
 */
@RestController
public class TestDummyController {

    /**
     * Метод, который всегда выбрасывает исключение MethodArgumentTypeMismatchException,
     * имитируя ситуацию, когда параметр в пути не может быть приведён к ожидаемому типу.
     * Используется для тестирования работы глобального обработчика ошибок.
     *
     * @throws MethodArgumentTypeMismatchException всегда, имитируя ошибку преобразования параметра
     */
    @GetMapping("/invalid")
    public void invalid() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("invalid");
        MethodParameter methodParameter = new MethodParameter(method, -1);

        throw new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "id",
                methodParameter,
                new NumberFormatException("For input string: \"abc\"")
        );
    }

    /**
     * Метод, при вызове которого выбрасывается IllegalArgumentException,
     * имитирующее ошибку при некорректном аргументе.
     *
     * @throws IllegalArgumentException всегда
     */
    @GetMapping("/illegal")
    public void illegal() {
        throw new IllegalArgumentException("bad arg");
    }

    /**
     * Метод, при вызове которого выбрасывается общее исключение RuntimeException,
     * имитирующее неожиданную ошибку приложения.
     *
     * @throws RuntimeException всегда
     */
    @GetMapping("/common")
    public void common() {
        throw new RuntimeException("boom");
    }
}
