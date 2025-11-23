//package io.github.habatoo.servicies.impl;
//
//import io.github.habatoo.dto.response.OrderDto;
//import io.github.habatoo.entity.Order;
//import io.github.habatoo.mappers.OrderMapper;
//import io.github.habatoo.repositories.OrderRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
///**
// * Параметризованные unit-тесты для OrderServiceImpl.
// * Проверяют все бизнес-кейсы: получение списка заказов, пустой список, получение по id,
// * отсутствие заказа по id, корректное преобразование через маппер.
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("Тест загрузки OrderServiceImpl")
//class OrderServiceImplTest {
//
//    @Mock
//    private OrderRepository orderRepository;
//    @Mock
//    private OrderMapper mapper;
//
//    private OrderServiceImpl service;
//
//    @BeforeEach
//    void setUp() {
//        service = new OrderServiceImpl(orderRepository, mapper);
//    }
//
//    /**
//     * Тест получения списка заказов: заказы присутствуют, корректное преобразование.
//     */
//    @ParameterizedTest
//    @MethodSource("ordersCases")
//    @DisplayName("Получение списка заказов — различные наборы")
//    void getOrdersTest(List<Order> inputOrders, List<OrderDto> expectedDtos) {
//        when(orderRepository.findAll()).thenReturn(inputOrders);
//        for (int i = 0; i < inputOrders.size(); i++) {
//            when(mapper.toDto(inputOrders.get(i))).thenReturn(expectedDtos.get(i));
//        }
//
//        List<OrderDto> result = service.getOrders();
//
//        assertEquals(expectedDtos, result);
//        verify(orderRepository).findAll();
//        inputOrders.forEach(order -> verify(mapper).toDto(order));
//    }
//
//    /**
//     * Тест получения списка заказов — пустой список.
//     */
//    @Test
//    @DisplayName("Получение списка заказов — пустой список")
//    void getOrdersEmptyTest() {
//        when(orderRepository.findAll()).thenReturn(List.of());
//
//        List<OrderDto> result = service.getOrders();
//
//        assertTrue(result.isEmpty());
//        verify(orderRepository).findAll();
//    }
//
//    /**
//     * Тест получения заказа по id — заказ найден.
//     */
//    @Test
//    @DisplayName("Получение заказа по id — заказ найден")
//    void getOrderFoundTest() {
//        Order order = new Order();
//        order.setId(12L);
//        OrderDto dto = mock(OrderDto.class);
//
//        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));
//        when(mapper.toDto(order)).thenReturn(dto);
//
//        OrderDto result = service.getOrder(12L, true);
//
//        assertEquals(dto, result);
//        verify(orderRepository).findById(12L);
//        verify(mapper).toDto(order);
//    }
//
//    /**
//     * Тест получения заказа по id — заказ не найден, выбрасывает исключение.
//     */
//    @Test
//    @DisplayName("Получение заказа по id — заказ не найден, выбрасывается исключение")
//    void getOrderNotFoundTest() {
//        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThrows(IllegalStateException.class, () -> service.getOrder(99L, false));
//        verify(orderRepository).findById(99L);
//    }
//
//    static Stream<Arguments> ordersCases() {
//        Order o1 = new Order();
//        o1.setId(3L);
//        o1.setTotalSum(BigDecimal.valueOf(250));
//        o1.setDateTime(LocalDateTime.now().minusDays(2));
//
//        Order o2 = new Order();
//        o2.setId(4L);
//        o2.setTotalSum(BigDecimal.valueOf(650));
//        o2.setDateTime(LocalDateTime.now().minusDays(1));
//
//        OrderDto dto1 = new OrderDto(3L, List.of(), BigDecimal.valueOf(250), LocalDateTime.now().minusDays(2));
//        OrderDto dto2 = new OrderDto(4L, List.of(), BigDecimal.valueOf(650), LocalDateTime.now().minusDays(1));
//
//        return Stream.of(
//                Arguments.of(List.of(o1, o2), List.of(dto1, dto2)),
//                Arguments.of(List.of(o1), List.of(dto1))
//        );
//    }
//}
