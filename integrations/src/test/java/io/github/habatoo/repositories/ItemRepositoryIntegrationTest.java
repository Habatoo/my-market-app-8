package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для репозитория ItemRepository.
 * Проверяет базовые CRUD операции: сохранение, выборку и удаление товаров.
 * Повторяющийся код вынесен в вспомогательные методы.
 */
@DataJpaTest
@DisplayName("Интеграционные тесты ItemRepository")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryIntegrationTest extends BaseTest {

    @Test
    @DisplayName("Поиск сохранённого товара по id")
    void saveItemHaveToBeFoundTest() {
        Item item = createItem("Title_1", "Description_1", "img/path1", BigDecimal.TEN);
        Long id = saveItem(item);

        Optional<Item> foundItem = itemRepository.findById(id);

        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getTitle()).isEqualTo("Title_1");
        assertThat(foundItem.get().getDescription()).isEqualTo("Description_1");
        assertThat(foundItem.get().getImgPath()).isEqualTo("img/path1");
    }

    @Test
    @DisplayName("Поиск всех товаров")
    void allItemsFoundTest() {
        Item item1 = createItem("Title_1", "Description_1", "img/path1", BigDecimal.TEN);
        Item item2 = createItem("Title_2", "Description_2", "img/path2", BigDecimal.ONE);

        saveItem(item1);
        saveItem(item2);

        List<Item> items = itemRepository.findAll();
        assertThat(items.stream().filter(item -> item.getTitle().contains("Title_")).toList()).hasSize(2);
    }

    @Test
    @DisplayName("Удаление товара по id")
    void deleteItemHaveTeBeNotFoundTest() {
        Item item = createItem("Title_1", "Description_1", "img/path1", BigDecimal.TEN);
        Long id = saveItem(item);

        itemRepository.deleteById(id);

        Optional<Item> deletedItem = itemRepository.findById(id);
        assertThat(deletedItem).isEmpty();
    }
}
