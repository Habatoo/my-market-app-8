package io.github.habatoo.storages;

import io.github.habatoo.dto.response.ItemDto;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Интерфейс абстракции для хранения и получения списков товаров в Redis.
 * Используется как кэш-слой для результатов поиска по каталогу.
 *
 * <p>Ключ кэша формируется на основании параметров поиска: исходной строки запроса,
 * размера страницы, номера страницы и параметров сортировки. Значением является список DTO
 * элементов каталога, сохранённый в Redis с заданным временем жизни (TTL), определяемым
 * конфигурацией приложения.</p>
 *
 * <p>Все операции выполняются в реактивной парадигме (Project Reactor) и возвращают
 * соответствующие типы {@code Mono}.</p>
 */
public interface RedisItemListStorage {

    /**
     * Получает список элементов каталога из Redis по вычисленному ключу.
     *
     * <p>Если данные отсутствуют в кэше, метод возвращает {@code Mono.empty()}.
     * Если данные найдены, возвращается {@code Mono<List<ItemDto>>}.</p>
     *
     * @param rawSearch  исходная поисковая строка, участвующая в построении ключа
     * @param pageSize   размер страницы результатов
     * @param pageNumber номер страницы (начиная с 0 или 1 — зависит от соглашения в приложении)
     * @param sort       параметры сортировки (поле и порядок), влияющие на ключ кэша
     * @return {@code Mono} с результатом: списком элементов или {@code Mono.empty()}, если кэш пуст
     */
    Mono<List<ItemDto>> getItems(
            String rawSearch,
            int pageSize,
            int pageNumber,
            Sort sort
    );

    /**
     * Сохраняет список элементов поиска в Redis по вычисленному ключу.
     *
     * <p>Запись выполняется с применением TTL, определённого конфигурацией приложения
     * (например, параметр {@code application.redis-ttl-minutes}).</p>
     *
     * @param rawSearch  исходная поисковая строка, участвующая в формировании ключа
     * @param pageSize   размер страницы результатов
     * @param pageNumber номер страницы
     * @param sort       параметры сортировки
     * @param dtos       список DTO элементов каталога, подлежащих сохранению
     * @return {@code Mono<Boolean>} — флаг успешности операции; {@code true}, если запись выполнена успешно
     */
    Mono<Boolean> saveItems(
            String rawSearch,
            int pageSize,
            int pageNumber,
            Sort sort,
            List<ItemDto> dtos
    );
}
