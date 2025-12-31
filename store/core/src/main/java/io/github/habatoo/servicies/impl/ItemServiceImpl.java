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
import io.github.habatoo.storages.RedisItemListStorage;
import io.github.habatoo.storages.RedisItemStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final ItemMapper mapper;
    private final RedisItemStorage redisItemStorage;
    private final RedisItemListStorage redisItemListStorage;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemsDtoResponse> getItems(GetItemsRequestDto request) {
        PageParams params = new PageParams(request);

        return getIsAuthMono().flatMap(isAuth ->
                fetchItemsList(params)
                        .zipWith(fetchTotalCount(params))
                        .flatMap(tuple -> assembleItemsResponse(
                                tuple.getT1(), tuple.getT2(), params, isAuth))
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> getItem(Long id) {
        return getIsAuthMono().flatMap(isAuth ->
                fetchSingleItem(id)
                        .flatMap(item -> assembleSingleItemResponse(item, isAuth))
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return cartService.changeNumberOfItems(request)
                .switchIfEmpty(Mono.defer(() -> repository.findById(request.getId()).map(mapper::toDto)))
                .zipWith(getIsAuthMono())
                .flatMap(tuple -> assembleSingleItemResponse(tuple.getT1(), tuple.getT2()));
    }

    private Mono<List<ItemDto>> fetchItemsList(PageParams p) {
        return redisItemListStorage.getItems(p.search, p.size, p.num, p.sort)
                .switchIfEmpty(Mono.defer(() -> findItemsInDb(p)));
    }

    private Mono<List<ItemDto>> findItemsInDb(PageParams p) {
        Pageable pageable = PageRequest.of(p.num - 1, p.size, p.sort);
        return (p.search.isBlank()
                ? repository.findAllBy(pageable)
                : repository.findByTitleContainingOrDescriptionContaining(p.search, p.search, pageable))
                .collectList()
                .map(mapper::toDto)
                .flatMap(items -> redisItemListStorage.saveItems(p.search, p.size, p.num, p.sort, items)
                        .thenReturn(items));
    }

    private Mono<ItemDto> fetchSingleItem(Long id) {
        return redisItemStorage.getItem(id)
                .switchIfEmpty(Mono.defer(() -> repository.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalStateException("Товар не найден: " + id)))
                        .map(mapper::toDto)
                        .flatMap(dto -> redisItemStorage.saveItem(id, dto).thenReturn(dto))));
    }

    private Mono<Long> fetchTotalCount(PageParams p) {
        return p.search.isBlank()
                ? repository.count()
                : repository.countByTitleContainingOrDescriptionContaining(p.search, p.search);
    }

    private Mono<ItemsDtoResponse> assembleItemsResponse(List<ItemDto> items, Long total, PageParams p, boolean isAuth) {
        Paging paging = getPaging(p.size, p.num, total);
        List<List<ItemDto>> rows = splitByRows(items, 3);

        if (!isAuth) {
            return Mono.just(buildAnonymousItemsResponse(rows, paging, items));
        }

        return cartService.getItemsInTheCart()
                .flatMap(cart -> loadCountsForItems(items, cart.id())
                        .map(counts -> ItemsDtoResponse.builder()
                                .itemsRows(rows)
                                .paging(paging)
                                .cart(cart)
                                .itemCounts(counts)
                                .isAuth(true)
                                .build()))
                .onErrorResume(e -> {
                    log.error("Ошибка получения корзины: ", e);
                    return Mono.just(buildAnonymousItemsResponse(rows, paging, items));
                });
    }

    private Mono<ItemDtoResponse> assembleSingleItemResponse(ItemDto item, boolean isAuth) {
        if (!isAuth) {
            return Mono.just(ItemDtoResponse.builder().item(item).cartCount(0).isAuth(false).build());
        }

        return cartService.getItemsInTheCart()
                .flatMap(cart -> cartItemRepository.findCountByCartIdAndItemId(cart.id(), item.id())
                        .defaultIfEmpty(0)
                        .map(count -> ItemDtoResponse.builder()
                                .item(item)
                                .cartCount(count)
                                .isAuth(true)
                                .build()));
    }

    private Mono<Boolean> getIsAuthMono() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> {
                    Authentication auth = ctx.getAuthentication();
                    boolean isAuth = auth != null && auth.isAuthenticated() &&
                            !(auth instanceof AnonymousAuthenticationToken);
                    log.info("Проверка Auth: name={}, isAuthenticated={}, type={}",
                            auth != null ? auth.getName() : "null",
                            isAuth,
                            auth != null ? auth.getClass().getSimpleName() : "null");

                    return isAuth;
                })
                .defaultIfEmpty(false)
                .doOnNext(res -> log.info("Флаг isAuth: {}", res));
    }

    private Mono<Map<Long, Integer>> loadCountsForItems(List<ItemDto> itemDtos, Long cartId) {
        return Flux.fromIterable(itemDtos)
                .flatMap(dto -> cartItemRepository.findCountByCartIdAndItemId(cartId, dto.id())
                        .defaultIfEmpty(0)
                        .map(cnt -> Map.entry(dto.id(), cnt)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private ItemsDtoResponse buildAnonymousItemsResponse(List<List<ItemDto>> rows, Paging paging, List<ItemDto> items) {
        Map<Long, Integer> zeroCounts = items.stream().collect(Collectors.toMap(ItemDto::id, i -> 0));
        return ItemsDtoResponse.builder()
                .itemsRows(rows)
                .paging(paging)
                .cart(obtainCartDto())
                .itemCounts(zeroCounts)
                .isAuth(false)
                .build();
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        List<List<ItemDto>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i += rowSize) {
            List<ItemDto> row = new ArrayList<>(items.subList(i, Math.min(items.size(), i + rowSize)));
            while (row.size() < rowSize) row.add(obtainEmptyItemDto());
            result.add(row);
        }
        return result;
    }

    private Paging getPaging(int size, int num, long total) {
        return Paging.builder()
                .total((int) total).pageSize(size).pageNumber(num)
                .hasPrevious(num > 1).hasNext((long) num * size < total)
                .build();
    }

    private CartDto obtainCartDto() { return new CartDto(
            -1L, List.of(), BigDecimal.ZERO); }

    private ItemDto obtainEmptyItemDto() { return new ItemDto(
            -1L, "", "", "", null, 0); }

    private Sort getSort(GetItemsRequestDto request) {
        if (request.getSort() == null) return Sort.unsorted();
        return switch (request.getSort()) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };
    }

    private class PageParams {
        final int size, num;
        final String search;
        final Sort sort;

        PageParams(GetItemsRequestDto r) {
            this.size = Optional.ofNullable(r.getPageSize()).orElse(5);
            this.num = Optional.ofNullable(r.getPageNumber()).orElse(1);
            this.search = Optional.ofNullable(r.getSearch()).orElse("").trim();
            this.sort = getSort(r);
        }
    }
}
