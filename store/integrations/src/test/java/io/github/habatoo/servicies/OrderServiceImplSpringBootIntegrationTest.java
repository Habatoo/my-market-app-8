package io.github.habatoo.servicies;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.entity.Order;
import io.github.habatoo.utils.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        createAndSaveOrder(BigDecimal.valueOf(100), LocalDateTime.now().minusDays(1)).block();
        createAndSaveOrder(BigDecimal.valueOf(200), LocalDateTime.now()).block();

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
        Order order = createAndSaveOrder(BigDecimal.valueOf(555), LocalDateTime.now()).block();

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
