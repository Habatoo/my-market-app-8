package io.github.habatoo.servicies;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.AutoConfigureDataR2dbc;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@AutoConfigureDataR2dbc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Интеграционный тест OrderServiceImpl — работа с заказами")
class OrderServiceImplSpringBootIntegrationTest extends BaseTest {

    @Autowired
    private OrderService orderService;

    /**
     * Получение всех заказов с корректным отображением в OrderDto.
     */
    @Test
    @DisplayName("Получение всех заказов — список OrderDto корректен (reactive)")
    void getOrdersListTest() {

        Order order1 = new Order();
        order1.setTotalSum(BigDecimal.valueOf(100));
        order1.setDateTime(LocalDateTime.now().minusDays(1));
        orderRepository.save(order1).block();

        Order order2 = new Order();
        order2.setTotalSum(BigDecimal.valueOf(200));
        order2.setDateTime(LocalDateTime.now());
        orderRepository.save(order2).block();

        List<OrderDto> dtos = orderService.getOrders()
                .collectList()
                .block();

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertTrue(dtos.stream().anyMatch(dto -> dto.totalSum().compareTo(BigDecimal.valueOf(100)) == 0));
        assertTrue(dtos.stream().anyMatch(dto -> dto.totalSum().compareTo(BigDecimal.valueOf(200)) == 0));
    }

    /**
     * Получение одного заказа по id.
     */
    @Test
    @DisplayName("Получение заказа по id — возвращается OrderDto (reactive)")
    void getOrderByIdTest() {

        Order order = new Order();
        order.setTotalSum(BigDecimal.valueOf(555));
        order.setDateTime(LocalDateTime.now());
        order = orderRepository.save(order).block();

        assertNotNull(order);
        assertNotNull(order.getId());

        OrderDto dto = orderService.getOrder(order.getId(), false).block();

        assertNotNull(dto);
        assertEquals(order.getId(), dto.id());
        assertEquals(order.getTotalSum(), dto.totalSum());
    }

    /**
     * Попытка загрузить несуществующий заказ должна дать ошибку.
     */
    @Test
    @DisplayName("Ошибка — заказ по id не найден (IllegalStateException, reactive)")
    void getOrderByIdNotFoundTest() {

        Long wrongId = -1234L;

        StepVerifier.create(orderService.getOrder(wrongId, false))
                .expectErrorMatches(e ->
                        e instanceof IllegalStateException &&
                                e.getMessage().contains("Заказ с id=" + wrongId + " не найден")
                )
                .verify();
    }
}
