package io.github.habatoo.configurations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@AutoConfiguration
@EnableR2dbcRepositories(basePackages = "io.github.habatoo.repositories")
public class R2dbcConfiguration {
}
