package io.github.habatoo.repositories;

import io.github.habatoo.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Репозиторий работы с пользователями.
 */
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByUsername(String username);

    Mono<User> findByExternalId(String externalId);
}
