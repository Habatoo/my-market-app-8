package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Репозиторий работы с товарами.
 */
public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {
}
