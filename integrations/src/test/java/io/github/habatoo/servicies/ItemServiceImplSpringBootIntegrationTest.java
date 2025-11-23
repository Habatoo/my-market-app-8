//package io.github.habatoo.servicies;
//
//import io.github.habatoo.Application;
//import io.github.habatoo.dto.enums.Action;
//import io.github.habatoo.dto.enums.Sort;
//import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
//import io.github.habatoo.dto.request.GetItemsRequestDto;
//import io.github.habatoo.dto.response.ItemDtoResponse;
//import io.github.habatoo.dto.response.ItemsDtoResponse;
//import io.github.habatoo.entity.Item;
//import io.github.habatoo.utils.BaseTest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Интеграционные тесты для ItemServiceImpl с использованием @SpringBootTest.
// * Проверяют поиск и сортировку товаров, получение карточки товара, пагинацию и кейсы ошибок.
// */
//@Transactional
//@ActiveProfiles("test")
//@SpringBootTest(classes = Application.class)
//@DisplayName("Интеграционный тест ItemServiceImpl — работа с витриной и товарами")
//class ItemServiceImplSpringBootIntegrationTest extends BaseTest {
//
//    @Autowired
//    private ItemService itemService;
//
//    @Autowired
//    private CartService cartService;
//
//    /**
//     * Тест — поиск и сортировка товаров.
//     */
//    @Test
//    @DisplayName("Поиск и сортировка — товары корректно фильтруются и сортируются")
//    void searchAndSortItemsTest() {
//        Item i1 = new Item();
//        i1.setTitle("Яблоко");
//        i1.setPrice(BigDecimal.valueOf(100));
//        Item i2 = new Item();
//        i2.setTitle("Груша");
//        i2.setPrice(BigDecimal.valueOf(300));
//        Item i3 = new Item();
//        i3.setTitle("Слива");
//        i3.setPrice(BigDecimal.valueOf(200));
//        itemRepository.save(i1);
//        itemRepository.save(i2);
//        itemRepository.save(i3);
//
//        GetItemsRequestDto req = GetItemsRequestDto.builder()
//                .search("груша")
//                .sort(Sort.PRICE)
//                .build();
//        ItemsDtoResponse itemsDto = itemService.getItems(req);
//
//        assertEquals(1, itemsDto.itemsRows().size());
//        assertTrue(itemsDto.itemsRows().get(0).stream().anyMatch(dto -> "Груша".equals(dto.title())));
//    }
//
//    /**
//     * Тест — пагинация товаров.
//     */
//    @Test
//    @DisplayName("Пагинация — выдаёт корректную страницу и Paging")
//    void paginationItemsTest() {
//        for (int i = 0; i < 10; i++) {
//            Item item = new Item();
//            item.setTitle("Товар " + i);
//            item.setPrice(BigDecimal.valueOf(10 + i));
//            itemRepository.save(item);
//        }
//
//        GetItemsRequestDto req = GetItemsRequestDto.builder()
//                .pageSize(5)
//                .pageNumber(2)
//                .sort(Sort.ALPHA)
//                .build();
//
//        ItemsDtoResponse dto = itemService.getItems(req);
//        assertEquals(3, dto.itemsRows().get(0).size());
//        assertEquals(10, dto.paging().total());
//        assertEquals(2, dto.paging().pageNumber());
//        assertTrue(dto.paging().hasPrevious());
//    }
//
//    /**
//     * Тест — карточка товара возвращается корректно.
//     */
//    @Test
//    @DisplayName("Получение карточки товара — данные соответствуют хранилищу, количество в корзине корректно")
//    void getItemCardTest() {
//        Item item = new Item();
//        item.setTitle("Банан");
//        item.setPrice(BigDecimal.valueOf(22));
//        item = itemRepository.save(item);
//
//        ChangeNumberOfItemsRequestDto dto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId())
//                .action(Action.PLUS)
//                .build();
//        cartService.changeNumberOfItems(dto);
//
//        ItemDtoResponse resp = itemService.getItem(item.getId());
//        assertEquals("Банан", resp.item().title());
//        assertEquals(Integer.valueOf(1), resp.cartCount());
//    }
//
//    /**
//     * Тест — ошибка при отсутствии товара.
//     */
//    @Test
//    @DisplayName("Ошибка — карточка товара по id не найдена")
//    void getItemNotFoundTest() {
//        assertThrows(IllegalStateException.class, () -> itemService.getItem(-555L));
//    }
//
//    /**
//     * Тест — изменение количества товара с itemService.changeNumberOfItemsFromPage.
//     */
//    @Test
//    @DisplayName("Изменение количества через changeNumberOfItemsFromPage возвращает актуальные данные")
//    void changeNumberOfItemsFromPageTest() {
//        Item item = new Item();
//        item.setTitle("Персик");
//        item.setPrice(BigDecimal.valueOf(77));
//        item = itemRepository.save(item);
//
//        ChangeNumberOfItemsRequestDto dto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId())
//                .action(Action.PLUS)
//                .build();
//        ItemDtoResponse resp = itemService.changeNumberOfItemsFromPage(dto);
//
//        assertEquals("Персик", resp.item().title());
//        assertEquals(Integer.valueOf(1), resp.cartCount());
//    }
//}
