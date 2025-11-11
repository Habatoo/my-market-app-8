package io.github.habatoo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Класс теста для основного класса приложения {@link Application}.
 * Проверяет корректный запуск метода main и вызов SpringApplication.run.
 */
@DisplayName("Тесты для класса Application")
class ApplicationTest {

    /**
     * Тест проверяет, что при запуске метода main
     * вызывается метод SpringApplication.run с правильными параметрами.
     */
    @Test
    @DisplayName("Проверка вызова SpringApplication.run при запуске main")
    void mainInvokesSpringApplicationRunTest() {
        try (MockedStatic<SpringApplication> mockedSpring = Mockito.mockStatic(SpringApplication.class)) {
            String[] args = {};
            Application.main(args);

            mockedSpring.verify(() -> SpringApplication.run(Application.class, args));
        }
    }
}
