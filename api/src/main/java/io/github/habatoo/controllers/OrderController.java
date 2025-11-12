package io.github.habatoo.controllers;

import io.github.habatoo.dto.response.OrderDto;
import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Контроллер для работы с заказами пользователя.
 * Позволяет просматривать список заказов, а также детальную информацию по каждому заказу.
 */
@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String ORDERS = "orders";

    private static final String ORDER = "order";

    private final OrderService orderService;

    /**
     * Отобразить список всех заказов пользователя.
     *
     * @param model модель для передачи атрибутов в шаблон
     * @return имя шаблона списка заказов
     */
    @GetMapping
    public String getOrderList(Model model) {
        log.info("GET /orders — запрос списка заказов пользователя");
        List<OrderDto> orders = orderService.getOrders();
        log.debug("Получено заказов: {}", orders.size());
        model.addAttribute(ORDERS, orders);

        return ORDERS;
    }

    /**
     * Отобразить страницу отдельного заказа по идентификатору.
     * Позволяет отметить новый заказ через параметр newOrder.
     *
     * @param id      идентификатор заказа
     * @param newOrder булевый флаг — новый ли это заказ (для отображения уведомлений и т.д.)
     * @param model   модель для передачи атрибутов
     * @return имя шаблона отдельного заказа
     */
    @GetMapping("/{id}")
    public String getOrder(
            @PathVariable Long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        log.info("GET /orders/{} — просмотр заказа, newOrder={}", id, newOrder);

        OrderDto order = orderService.getOrder(id, newOrder);
        log.debug("Получен заказ: id={}, itemsCount={}, totalSum={}",
                order.id(), order.items() == null ? 0 : order.items().size(), order.totalSum());

        model.addAttribute(ORDER, order);
        model.addAttribute("newOrder", newOrder);

        return ORDER;
    }
}
