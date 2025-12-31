package io.github.habatoo.storages;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Интерфейс абстракции для кэширования и получения отдельных товаров (Item) в Redis.
 * Используется как кэш-слой для быстрого доступа к данным по идентификатору товара.
 *
 * <p>Ключом в Redis выступает идентификатор товара ({@code id}). Значением является
 * объект {@link java.math.BigDecimal}, сохранённый с использованием настроенного времени жизни (TTL),
 * определяемого конфигурацией приложения.</p>
 *
 * <p>Все операции выполняются в реактивной парадигме (Project Reactor) и возвращают
 * реактивный тип {@code Mono}.</p>
 */
public interface RedisBalanceStorage {

    /**
     * Получает объект товара из Redis по его идентификатору.
     *
     * <p>Если запись отсутствует в кэше, возвращается {@code Mono.empty()}.
     * Если значение найдено, возвращается {@code Mono<ItemDto>}.</p>
     *
     * @param username имя пользователя, под которым сохранится значение в Redis
     * @return {@code Mono} с найденным объектом товара или {@code Mono.empty()}, если кэш пуст
     */
    Mono<BigDecimal> getBalanceByName(String username);

    /**
     * Сохраняет объект товара в Redis по заданному идентификатору.
     *
     * <p>Запись производится с использованием стандартного TTL,
     * заданного в конфигурации (например, {@code application.redis-ttl-minutes}).</p>
     *
     * @param username имя пользователя, под которым сохранится значение в Redis
     * @param balance  сумма обновленного баланса, который требуется сохранить
     * @return {@code Mono<Boolean>} — результат операции: {@code true} при успешной записи
     */
    Mono<Boolean> saveBalanceByName(String username, BigDecimal balance);
}

