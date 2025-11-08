package io.github.habatoo.repositories;

import io.github.habatoo.entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для репозитория ItemRepository.
 * Проверяет базовые CRUD операции: сохранение, выборку и удаление товаров.
 * Повторяющийся код вынесен в вспомогательные методы.
 */
@ActiveProfiles("test")
@DisplayName("Интеграционные тесты ItemRepository")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void cleanUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

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

    /**
     * Создаёт новый экземпляр Item с указанными параметрами.
     *
     * @param title       Название товара
     * @param description Описание товара
     * @param imgPath     Путь к изображению
     * @param price       Цена товара
     * @return объект Item
     */
    private Item createItem(String title, String description, String imgPath, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setImgPath(imgPath);
        item.setPrice(price);
        return item;
    }

    /**
     * Сохраняет товар и возвращает его id.
     *
     * @param item экземпляр Item
     * @return id сохранённого Item
     */
    private Long saveItem(Item item) {
        return itemRepository.save(item).getId();
    }
}
