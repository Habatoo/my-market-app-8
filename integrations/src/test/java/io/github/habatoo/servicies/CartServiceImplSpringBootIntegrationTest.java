//package io.github.habatoo.servicies;
//
//import io.github.habatoo.Application;
//import io.github.habatoo.dto.enums.Action;
//import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
//import io.github.habatoo.dto.response.CartDto;
//import io.github.habatoo.dto.response.ItemDto;
//import io.github.habatoo.entity.Cart;
//import io.github.habatoo.entity.CartItem;
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
// * Интеграционные тесты для CartServiceImpl с использованием @SpringBootTest.
// * Покрывают ключевые ситуации работы с корзиной: добавление, удаление, изменение количества товаров, пустая корзина и ошибка отсутствия товара.
// */
//@Transactional
//@ActiveProfiles("test")
//@SpringBootTest(classes = Application.class)
//@DisplayName("Интеграционный тест CartServiceImpl — работа с корзиной")
//class CartServiceImplSpringBootIntegrationTest extends BaseTest {
//
//    @Autowired
//    private CartService cartService;
//
//    /**
//     * Тест — успешное добавление нового товара в корзину.
//     */
//    @Test
//    @DisplayName("Добавление товара — товар появляется в корзине, количество и сумма корректны")
//    void addItemToCartSuccessTest() {
//        Item item = createAndSaveItem("CartItem1", BigDecimal.valueOf(70));
//        ChangeNumberOfItemsRequestDto dto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId())
//                .action(Action.PLUS)
//                .build();
//
//        ItemDto itemDto = cartService.changeNumberOfItems(dto);
//
//        Cart cart = cartRepository.findAll().get(0);
//        assertFalse(cart.getItems().isEmpty());
//        CartItem cartItem = cart.getItems().get(0);
//        assertEquals(item.getId(), cartItem.getItem().getId());
//        assertEquals(1, cartItem.getCount());
//        assertEquals(item.getPrice(), cartItem.getPrice());
//        assertEquals(item.getPrice(), cart.getTotal());
//        assertNotNull(itemDto);
//    }
//
//    /**
//     * Тест — увеличение и уменьшение количества существующего товара.
//     */
//    @Test
//    @DisplayName("Изменение количества товара — увеличение и уменьшение, удаление при 0")
//    void incrementAndDecrementItemCountTest() {
//        Item item = createAndSaveItem("CartItem1", BigDecimal.valueOf(99));
//        ChangeNumberOfItemsRequestDto plusDto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId()).action(Action.PLUS).build();
//        cartService.changeNumberOfItems(plusDto);
//        cartService.changeNumberOfItems(plusDto);
//        ChangeNumberOfItemsRequestDto minusDto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId()).action(Action.MINUS).build();
//        cartService.changeNumberOfItems(minusDto);
//
//        Cart cart = cartRepository.findAll().get(0);
//        CartItem ci = cart.getItems().get(0);
//        assertEquals(1, ci.getCount());
//        assertEquals(item.getPrice(), cart.getTotal());
//
//        cartService.changeNumberOfItems(minusDto);
//
//        Cart cartAfter = cartRepository.findAll().get(0);
//        assertTrue(cartAfter.getItems().isEmpty());
//        assertEquals(BigDecimal.ZERO, cartAfter.getTotal());
//    }
//
//    /**
//     * Тест — возвращается корректный CartDto для корзины.
//     */
//    @Test
//    @DisplayName("Запрос содержимого корзины — получаем CartDto, сумма и товары корректны")
//    void getItemsInTheCartTest() {
//        Item item = createAndSaveItem("CartItem1", BigDecimal.valueOf(33));
//        ChangeNumberOfItemsRequestDto dto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId()).action(Action.PLUS).build();
//
//        cartService.changeNumberOfItems(dto);
//
//        CartDto cartDto = cartService.getItemsInTheCart();
//        assertNotNull(cartDto);
//        assertEquals(1, cartDto.items().size());
//        assertEquals(BigDecimal.valueOf(33), cartDto.total());
//    }
//
//    /**
//     * Тест — попытка изменить количество несуществующего товара выбрасывает исключение.
//     */
//    @Test
//    @DisplayName("Ошибка — попытка изменить количество несуществующего товара")
//    void changeNumberOfItemsNotFoundTest() {
//        ChangeNumberOfItemsRequestDto dto = ChangeNumberOfItemsRequestDto.builder()
//                .id(-1234L).action(Action.PLUS).build();
//
//        assertThrows(IllegalStateException.class, () -> cartService.changeNumberOfItems(dto));
//    }
//
//    /**
//     * Тест — изменение количества через changeNumberOfItemsFromCart синхронизировано с содержимым корзины.
//     */
//    @Test
//    @DisplayName("Изменение через changeNumberOfItemsFromCart возвращает актуальный CartDto")
//    void changeNumberOfItemsFromCartTest() {
//        Item item = createAndSaveItem("CartItem1", BigDecimal.valueOf(55));
//        ChangeNumberOfItemsRequestDto plusDto = ChangeNumberOfItemsRequestDto.builder()
//                .id(item.getId()).action(Action.PLUS).build();
//
//        CartDto afterAdd = cartService.changeNumberOfItemsFromCart(plusDto);
//
//        assertEquals(BigDecimal.valueOf(55), afterAdd.total());
//    }
//}
