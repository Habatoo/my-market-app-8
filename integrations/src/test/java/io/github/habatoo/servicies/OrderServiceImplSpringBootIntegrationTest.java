//package io.github.habatoo.servicies;
//
//import io.github.habatoo.Application;
//import io.github.habatoo.dto.response.OrderDto;
//import io.github.habatoo.entity.Order;
//import io.github.habatoo.utils.BaseTest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Интеграционные тесты для OrderServiceImpl с использованием @SpringBootTest.
// * Покрывают получение всех заказов, получение заказа по id, отсутствие заказа.
// */
//@Transactional
//@ActiveProfiles("test")
//@SpringBootTest(classes = Application.class)
//@DisplayName("Интеграционный тест OrderServiceImpl — работа с заказами")
//class OrderServiceImplSpringBootIntegrationTest extends BaseTest {
//
//    @Autowired
//    private OrderService orderService;
//
//    /**
//     * Тест — успешное получение всех заказов, корректное преобразование через маппер.
//     */
//    @Test
//    @DisplayName("Получение всех заказов — список OrderDto корректен")
//    void getOrdersListTest() {
//        Order order1 = new Order();
//        order1.setTotalSum(BigDecimal.valueOf(100));
//        order1.setDateTime(LocalDateTime.now().minusDays(1));
//        orderRepository.save(order1);
//
//        Order order2 = new Order();
//        order2.setTotalSum(BigDecimal.valueOf(200));
//        order2.setDateTime(LocalDateTime.now());
//        orderRepository.save(order2);
//
//        List<OrderDto> dtos = orderService.getOrders();
//
//        assertEquals(2, dtos.size());
//        assertTrue(dtos.stream().anyMatch(dto -> dto.totalSum().compareTo(BigDecimal.valueOf(100)) == 0));
//        assertTrue(dtos.stream().anyMatch(dto -> dto.totalSum().compareTo(BigDecimal.valueOf(200)) == 0));
//    }
//
//    /**
//     * Тест — успешное получение заказа по id.
//     */
//    @Test
//    @DisplayName("Получение заказа по id — возвращается OrderDto")
//    void getOrderByIdTest() {
//        Order order = new Order();
//        order.setTotalSum(BigDecimal.valueOf(555));
//        order.setDateTime(LocalDateTime.now());
//        order = orderRepository.save(order);
//
//        OrderDto dto = orderService.getOrder(order.getId(), false);
//
//        assertNotNull(dto);
//        assertEquals(order.getId(), dto.id());
//        assertEquals(order.getTotalSum(), dto.totalSum());
//    }
//
//    /**
//     * Тест — попытка получить несуществующий заказ приводит к IllegalStateException.
//     */
//    @Test
//    @DisplayName("Ошибка — заказ по id не найден (IllegalStateException)")
//    void getOrderByIdNotFoundTest() {
//        Long wrongId = -1234L;
//        assertThrows(IllegalStateException.class, () -> orderService.getOrder(wrongId, false));
//    }
//}
