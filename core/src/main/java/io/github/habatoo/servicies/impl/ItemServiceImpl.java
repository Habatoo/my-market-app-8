package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.CartItemRepository;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
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
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final ItemMapper mapper;

    public ItemServiceImpl(
            ItemRepository repository,
            CartItemRepository cartItemRepository,
            CartService cartService,
            ItemMapper mapper) {
        this.repository = repository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemsDtoResponse> getItems(GetItemsRequestDto request) {
        log.debug("Запрошено получение товаров: request={}", request);

        Mono<CartDto> cartMono = obtainCart();

        return Mono.fromCallable(repository::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(all -> {

                    List<Item> filtered = all;
                    if (request.getSearch() != null && !request.getSearch().isBlank()) {
                        filtered = getFiltered(request, filtered);
                    }

                    if (request.getSort() != null) {
                        filtered = getItemList(request, filtered);
                    }

                    Result result = getResult(request, filtered);

                    return cartMono.map(cart -> {
                        Map<Long, Integer> itemCounts = obtainItemCounts(result.items(), cart.id());

                        return ItemsDtoResponse.builder()
                                .itemsRows(result.itemsRows())
                                .cart(cart)
                                .paging(result.paging())
                                .itemCounts(itemCounts)
                                .build();
                    });
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> getItem(Long id) {
        log.debug("Запрошено получение товара по id={}", id);

        ItemDto item = repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> {
                    log.error("Товар с id={} не найден", id);

                    return new IllegalStateException("Товар с id=%d не найден".formatted(id));
                });
        Mono<CartDto> cartMono = obtainCart();

        return cartMono.map(cart -> {
                    Long cartId = cart.id();
                    Integer cartCount = cartItemRepository.findCountByCartIdAndItemId(cartId, id);

                    if (cartCount == null) {
                        cartCount = 0;
                    }

                    log.info("Товар получен: id={}, в корзине={}", id, cartCount);

                    return ItemDtoResponse.builder()
                            .item(item)
                            .cartCount(cartCount)
                            .build();
                }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        log.debug("Запрошено изменение товаров из страницы: request={}", request);

        Mono<CartDto> cartMono = obtainCart();

        return cartService.changeNumberOfItems(request)
                .flatMap(item -> cartMono.map(cart -> {
                    Long cartId = cart.id();
                    Integer cartCount = getCartCount(cartId, request.getId());

                    log.info("Изменение количества товара в корзине: itemId={}, newCount={}",
                            request.getId(), cartCount);

                    return ItemDtoResponse.builder()
                            .item(item)
                            .cartCount(cartCount)
                            .build();
                }));
    }

    private Integer getCartCount(Long cartId, Long id) {
        Integer cartCount = cartItemRepository.findCountByCartIdAndItemId(cartId, id);
        if (cartCount == null) {
            cartCount = 0;
        }

        return cartCount;
    }

    private Map<Long, Integer> obtainItemCounts(List<ItemDto> items, Long cartId) {
        Map<Long, Integer> itemCounts = new HashMap<>();
        for (ItemDto item : items) {
            Integer count = cartItemRepository.findCountByCartIdAndItemId(cartId, item.id());
            itemCounts.put(item.id(), count == null ? 0 : count);
        }

        return itemCounts;
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

    private Mono<CartDto> obtainCart() {
        return cartService.getItemsInTheCart();
    }

    private List<Item> getItemList(GetItemsRequestDto request, List<Item> filtered) {
        switch (request.getSort()) {
            case ALPHA -> filtered = filtered.stream()
                    .sorted(Comparator.comparing(Item::getTitle))
                    .toList();
            case PRICE -> filtered = filtered.stream()
                    .sorted(Comparator.comparing(Item::getPrice))
                    .toList();
        }
        return filtered;
    }

    private List<Item> getFiltered(GetItemsRequestDto request, List<Item> filtered) {
        String lower = request.getSearch().trim().toLowerCase();
        filtered = filtered.stream()
                .filter(i -> i.getTitle().toLowerCase().contains(lower)
                        || (i.getDescription() != null
                        && i.getDescription().toLowerCase().contains(lower)))
                .toList();

        return filtered;
    }

    private Result getResult(GetItemsRequestDto request, List<Item> filtered) {
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;

        int total = filtered.size();
        int from = Math.max((pageNumber - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, total);

        List<Item> page = from < total ? filtered.subList(from, to) : List.of();
        List<ItemDto> items = mapper.toDto(page);
        List<List<ItemDto>> itemsRows = splitByRows(items, 3);

        Paging paging = Paging.builder()
                .total(total)
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(pageNumber * pageSize < total)
                .build();

        return new Result(items, itemsRows, paging);
    }

    private record Result(List<ItemDto> items, List<List<ItemDto>> itemsRows, Paging paging) {
    }
}
