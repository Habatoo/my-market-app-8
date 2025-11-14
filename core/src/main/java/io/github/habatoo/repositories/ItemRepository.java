package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий работы с товарами.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {
}
