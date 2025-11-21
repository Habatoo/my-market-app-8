package io.github.habatoo.controllers;

import io.github.habatoo.servicies.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
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

    private static final String NOT_FOUND = "404";

    private final OrderService orderService;

    /**
     * Отобразить список всех заказов пользователя.
     *
     * @return имя шаблона списка заказов
     */
    @GetMapping
    public Mono<Rendering> getOrderList(Model model) {
        log.info("GET /orders — запрос списка заказов пользователя");

        return Mono.just(
                Rendering.view("/orders")
                        .modelAttribute(ORDERS, orderService.getOrders())
                        .build()
        );
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
    public Mono<String> getOrder(
            @PathVariable Long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        log.info("GET /orders/{} — просмотр заказа, newOrder={}", id, newOrder);

        return orderService.getOrder(id, newOrder)
                .doOnNext(order -> model.addAttribute(ORDER, order))
                .doOnNext(order -> model.addAttribute("newOrder", newOrder))
                .map(order -> ORDER)
                .defaultIfEmpty(NOT_FOUND);
    }
}
