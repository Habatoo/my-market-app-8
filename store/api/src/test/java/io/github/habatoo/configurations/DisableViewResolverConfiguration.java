package io.github.habatoo.configurations;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Тестовая конфигурация для перехвата резолверов.
 */
@TestConfiguration
public class DisableViewResolverConfiguration {

    @Bean
    public ITemplateResolver templateResolver() {
        return new StringTemplateResolver();
    }
}
