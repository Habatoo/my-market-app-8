package io.github.habatoo.servicies;

import io.github.habatoo.exceptions.InsufficientFundsException;
import io.github.habatoo.exceptions.PaymentServiceUnavailableException;
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
     * <p>
     * Выполняет следующую последовательность действий:
     * <ol>
     * <li>Поиск корзины по ID.</li>
     * <li>Загрузка товаров корзины (ошибка, если пусто).</li>
     * <li>Расчет итоговой суммы.</li>
     * <li>Проведение платежа через внешний API.</li>
     * <li>Сохранение заказа и позиций в БД.</li>
     * <li>Очистка корзины.</li>
     * </ol>
     *
     * @param id идентификатор корзины пользователя.
     * @return {@link Mono}, содержащий идентификатор созданного заказа (OrderId).
     * @throws IllegalStateException              если корзина не найдена или пуста.
     * @throws InsufficientFundsException         если платеж отклонен (недостаточно средств).
     * @throws PaymentServiceUnavailableException если сервис платежей недоступен.
     */
    Mono<Long> buy(Long id);
}
