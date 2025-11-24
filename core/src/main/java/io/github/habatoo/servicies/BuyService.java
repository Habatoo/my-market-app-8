package io.github.habatoo.servicies;

import reactor.core.publisher.Mono;

/**
 * Интерфейс для осуществления покупки.
 * Предоставляет бизнес-логику для совершения покупки.
 */
public interface BuyService {

    /**
     * Эндпоинт совершения заказа
     * POST /buy
     * Редирект: redirect:/orders/{id}?newOrder=true, где id — идентификатор созданного заказа.
     *
     * @param id идентификатор заказа
     */
    Mono<Long> buy(Long id);
}
