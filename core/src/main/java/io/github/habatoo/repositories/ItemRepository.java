package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Репозиторий работы с товарами.
 */
public interface ItemRepository extends R2dbcRepository<Item, Long> {

    Flux<Item> findByTitleContainingOrDescriptionContaining(
            String title, String description, Pageable pageable
    );

    Flux<Item> findAllBy(Pageable pageable);

    Mono<Long> countByTitleContainingOrDescriptionContaining(
            String title, String description
    );
}
