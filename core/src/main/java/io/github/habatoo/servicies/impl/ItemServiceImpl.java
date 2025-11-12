package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
@Slf4j
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final CartService cartService;
    private final ItemMapper mapper;

    public ItemServiceImpl(
            ItemRepository repository,
            CartService cartService,
            ItemMapper mapper) {
        this.repository = repository;
        this.cartService = cartService;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemsDtoResponse getItems(GetItemsRequestDto request) {
        log.debug("Запрошено получение товаров: request={}", request);

        List<Item> all = repository.findAll();
        log.info("Всего товаров в репозитории: {}", all.size());

        List<Item> filtered = all;
        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            String lower = request.getSearch().trim().toLowerCase();
            log.debug("Поиск по строке: '{}'", lower);
            filtered = filtered.stream()
                    .filter(i -> i.getTitle().toLowerCase().contains(lower)
                            || (i.getDescription() != null && i.getDescription().toLowerCase().contains(lower)))
                    .toList();
            log.info("Найдено товаров после фильтрации: {}", filtered.size());
        }

        if (request.getSort() != null) {
            log.debug("Сортировка: {}", request.getSort());
            switch (request.getSort()) {
                case ALPHA -> {
                    filtered = filtered.stream()
                            .sorted(Comparator.comparing(Item::getTitle)).toList();
                    log.debug("Сортировка ALPHA выполнена");
                }
                case PRICE -> {
                    filtered = filtered.stream()
                            .sorted(Comparator.comparing(Item::getPrice)).toList();
                    log.debug("Сортировка PRICE выполнена");
                }
                default -> log.debug("Неизвестный тип сортировки: {}", request.getSort());
            }
        }

        int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
        int from = (pageNumber - 1) * pageSize;
        int to = Math.min(from + pageSize, filtered.size());

        log.debug(
                "Пагинация: pageNumber={}, pageSize={}, from={}, to={}",
                pageNumber, pageSize, from, to);

        List<Item> page = from < filtered.size() ? filtered.subList(from, to) : Collections.emptyList();
        log.info("Товаров на странице: {}", page.size());

        List<ItemDto> items = mapper.toDto(page);
        log.debug("Преобразование товаров в DTO выполнено");

        List<List<ItemDto>> itemsRows = splitByRows(items, 3);
        log.trace(
                "Формирование строк товаров для фронта завершено, строк: {}",
                itemsRows.size());

        CartDto cart = obtainCart();
        log.debug(
                "Корзина получена: cartId={}, товаров в корзине={}",
                cart.id(), cart.items().size());

        Paging paging = Paging.builder()
                .total(filtered.size())
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(pageNumber * pageSize < filtered.size())
                .build();

        log.debug("Paging info: {}", paging);

        ItemsDtoResponse response = ItemsDtoResponse.builder()
                .itemsRows(itemsRows)
                .cart(cart)
                .paging(paging)
                .build();

        log.info(
                "Подготовлен ответ на запрос товаров: itemsRows={}, totalFiltered={}",
                itemsRows.size(), filtered.size());
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDtoResponse getItem(Long id) {
        log.debug("Запрошено получение товара по id={}", id);
        ItemDto item = repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.error("Товар с id={} не найден", id);
                    return new IllegalStateException("Товар с id=%d не найден".formatted(id));
                });
        CartDto cart = obtainCart();
        Integer cartCount = cart.getCountByItemId(id);

        log.info("Товар получен: id={}, в корзине={}", id, cartCount);

        return ItemDtoResponse.builder()
                .item(item)
                .cartCount(cartCount)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDtoResponse changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        log.debug("Запрошено изменение товаров из страницы: request={}", request);

        ItemDto item = cartService.changeNumberOfItems(request);
        CartDto cart = obtainCart();
        Integer cartCount = cart.getCountByItemId(request.getId());

        log.info(
                "Изменение количества товара в корзине: itemId={}, newCount={}",
                request.getId(), cartCount);

        return ItemDtoResponse.builder()
                .item(item)
                .cartCount(cartCount)
                .build();
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        int totalRows = (int) Math.ceil((double) items.size() / rowSize);
        log.debug(
                "splitByRows: itemsTotal={}, rowSize={}, totalRows={}",
                items.size(), rowSize, totalRows);

        return IntStream.range(0, totalRows)
                .mapToObj(i -> getSubList(items, rowSize, i))
                .map(subList -> getDtoList(rowSize, subList))
                .collect(Collectors.toList());
    }

    private List<ItemDto> getSubList(List<ItemDto> items, int rowSize, int i) {
        List<ItemDto> result = items.subList(i * rowSize, Math.min(items.size(), (i + 1) * rowSize));
        log.trace(
                "getSubList: from={}, to={}, size={}",
                i * rowSize, Math.min(items.size(), (i + 1) * rowSize), result.size());
        return result;
    }

    private List<ItemDto> getDtoList(int rowSize, List<ItemDto> subList) {
        List<ItemDto> list = new ArrayList<>(subList);
        while (list.size() < rowSize) {
            list.add(new ItemDto(-1L, "", "", "", null, 0));
        }
        log.trace("getDtoList: rowSize={}, filledSize={}", rowSize, list.size());
        return list;
    }

    private CartDto obtainCart() {
        CartDto cart = cartService.getItemsInTheCart();
        log.debug("obtainCart: cartId={}, itemsCount={}", cart.id(), cart.items().size());
        return cart;
    }
}
