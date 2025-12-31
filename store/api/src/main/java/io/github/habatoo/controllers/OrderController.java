package io.github.habatoo.controllers;

import io.github.habatoo.servicies.OrderService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

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
     * @return имя шаблона списка заказов
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<String> getOrderList(Model model) {
        log.info("GET /orders — запрос списка заказов пользователя");

        return orderService.getOrders()
                .collectList()
                .doOnNext(list -> log.debug("Найдено {} заказов", list.size()))
                .doOnNext(list -> model.addAttribute(ORDERS, list))
                .thenReturn(ORDERS);
    }

    /**
     * Отобразить страницу отдельного заказа по идентификатору.
     * Позволяет отметить новый заказ через параметр newOrder.
     *
     * @param id       идентификатор заказа
     * @param newOrder булевый флаг — новый ли это заказ (для отображения уведомлений и т.д.)
     * @param model    модель для передачи атрибутов
     * @return имя шаблона отдельного заказа
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Mono<String> getOrder(
            @PathVariable @Positive Long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        log.info("GET /orders/{} — просмотр заказа, newOrder={}", id, newOrder);

        return orderService.getOrder(id, newOrder)
                .doOnNext(order -> {
                    model.addAttribute(ORDER, order);
                    model.addAttribute("newOrder", newOrder);
                })
                .thenReturn(ORDER);
    }
}
