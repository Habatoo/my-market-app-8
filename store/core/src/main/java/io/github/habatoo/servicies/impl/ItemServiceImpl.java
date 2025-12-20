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

        int pageSize = Optional.ofNullable(request.getPageSize()).orElse(5);
        int pageNumber = Optional.ofNullable(request.getPageNumber()).orElse(1);
        String rawSearch = Optional.ofNullable(request.getSearch()).orElse("").trim();
        Sort sort = getSort(request);

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Mono<Boolean> isAuthMono = getIsAuthMono();

        Mono<List<ItemDto>> itemsMono =
                redisItemListStorage.getItems(rawSearch, pageSize, pageNumber, sort)
                        .switchIfEmpty(
                                (rawSearch.isBlank()
                                        ? repository.findAllBy(pageable)
                                        : repository.findByTitleContainingOrDescriptionContaining(
                                        rawSearch, rawSearch, pageable))
                                        .collectList()
                                        .map(mapper::toDto)
                                        .doOnNext(items ->
                                                redisItemListStorage.saveItems(
                                                        rawSearch, pageSize, pageNumber, sort, items
                                                ).thenReturn(items)
                                        )
                        );

        Mono<Long> totalMono =
                rawSearch.isBlank()
                        ? repository.count()
                        : repository.countByTitleContainingOrDescriptionContaining(rawSearch, rawSearch);

        return Mono.zip(itemsMono, totalMono, isAuthMono)
                .flatMap(tuple -> {
                    List<ItemDto> items = tuple.getT1();
                    long total = tuple.getT2();
                    boolean isAuth = tuple.getT3();

                    Paging paging = getPaging(pageSize, pageNumber, total);
                    List<List<ItemDto>> rows = splitByRows(items, 3);

                    if (!isAuth) {
                        Map<Long, Integer> zeroCounts = items.stream()
                                .collect(Collectors.toMap(
                                        ItemDto::id,
                                        item -> 0
                                ));

                        return Mono.just(
                                ItemsDtoResponse.builder()
                                        .itemsRows(rows)
                                        .paging(paging)
                                        .cart(obtainCartDto())
                                        .itemCounts(zeroCounts)
                                        .isAuth(false)
                                        .build()
                        );
                    }

                    return cartService.getItemsInTheCart()
                            .flatMap(cart ->
                                    loadCountsForItems(items, cart.id())
                                            .map(counts ->
                                                    ItemsDtoResponse.builder()
                                                            .itemsRows(rows)
                                                            .paging(paging)
                                                            .cart(cart)
                                                            .itemCounts(counts)
                                                            .isAuth(true)
                                                            .build()
                                            )
                            );
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> getItem(Long id) {

        Mono<ItemDto> itemMono =
                redisItemStorage.getItem(id)
                        .switchIfEmpty(
                                repository.findById(id)
                                        .switchIfEmpty(Mono.error(
                                                new IllegalStateException("Товар с id=" + id + " не найден")
                                        ))
                                        .map(mapper::toDto)
                                        .flatMap(dto ->
                                                redisItemStorage.saveItem(id, dto).thenReturn(dto)
                                        )
                        );

        Mono<Boolean> isAuthMono = getIsAuthMono();

        return Mono.zip(itemMono, isAuthMono)
                .flatMap(tuple -> {
                    ItemDto item = tuple.getT1();
                    boolean isAuth = tuple.getT2();

                    if (!isAuth) {
                        return Mono.just(
                                ItemDtoResponse.builder()
                                        .item(item)
                                        .cartCount(0)
                                        .build()
                        );
                    }

                    return cartService.getItemsInTheCart()
                            .flatMap(cart ->
                                    cartItemRepository
                                            .findCountByCartIdAndItemId(cart.id(), item.id())
                                            .defaultIfEmpty(0)
                                            .map(count ->
                                                    ItemDtoResponse.builder()
                                                            .item(item)
                                                            .cartCount(count)
                                                            .build()
                                            )
                            );
                });
    }

    private Mono<Boolean> getIsAuthMono() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().isAuthenticated())
                .defaultIfEmpty(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return cartService.changeNumberOfItems(request)
                .switchIfEmpty(Mono.defer(() -> repository.findById(request.getId()).map(mapper::toDto)))
                .zipWith(cartService.getItemsInTheCart())
                .flatMap(tuple -> buildItemResponse(
                        tuple.getT1(), tuple.getT2(), tuple.getT1().id()));
    }

    private Mono<Map<Long, Integer>> loadCountsForItems(List<ItemDto> itemDtos, Long cartId) {
        return Flux.fromIterable(itemDtos)
                .flatMap(itemDto ->
                        cartItemRepository.findCountByCartIdAndItemId(cartId, itemDto.id())
                                .defaultIfEmpty(0)
                                .map(cnt -> Map.entry(itemDto.id(), cnt))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private Mono<ItemsDtoResponse> loadItemsFromDb(
            String search,
            int pageSize,
            int pageNumber,
            Sort sort,
            Mono<CartDto> cartMono) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);
        Flux<Item> itemsFlux = search.isBlank()
                ? repository.findAllBy(pageable)
                : repository.findByTitleContainingOrDescriptionContaining(search, search, pageable);
        Mono<Long> totalMono = search.isBlank()
                ? repository.count()
                : repository.countByTitleContainingOrDescriptionContaining(search, search);

        return itemsFlux.collectList()
                .map(mapper::toDto)
                .zipWith(cartMono)
                .flatMap(tuple -> loadCountsForItems(tuple.getT1(), tuple.getT2().id())
                        .map(countMap -> Tuples.of(tuple.getT1(), tuple.getT2(), countMap))
                )
                .zipWith(totalMono)
                .map(tuple -> {
                    List<ItemDto> items = tuple.getT1().getT1();
                    CartDto cart = tuple.getT1().getT2();
                    Map<Long, Integer> countMap = tuple.getT1().getT3();
                    long total = tuple.getT2();

                    return buildItemsResponse(
                            splitByRows(items, 3), cart, getPaging(pageSize, pageNumber, total), countMap);
                });
    }

    private ItemsDtoResponse buildItemsResponse(
            List<List<ItemDto>> rows,
            CartDto cart,
            Paging paging,
            Map<Long, Integer> itemCounts
    ) {
        return ItemsDtoResponse.builder()
                .itemsRows(rows)
                .cart(cart)
                .paging(paging)
                .itemCounts(itemCounts)
                .build();
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        int totalRows = (int) Math.ceil((double) items.size() / rowSize);

        List<List<ItemDto>> result = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) {
            int from = i * rowSize;
            int to = Math.min(items.size(), (i + 1) * rowSize);
            List<ItemDto> sub = new ArrayList<>(items.subList(from, to));

            while (sub.size() < rowSize) {
                sub.add(obtainEmptyItemDto());
            }
            result.add(sub);
        }
        return result;
    }

    private Mono<ItemDtoResponse> buildItemResponse(ItemDto item, CartDto cart, Long itemId) {
        return cartItemRepository.findCountByCartIdAndItemId(cart.id(), itemId)
                .defaultIfEmpty(0)
                .map(cnt -> ItemDtoResponse.builder()
                        .item(item)
                        .cartCount(cnt)
                        .build());
    }

    private Paging getPaging(int pageSize, int pageNumber, long total) {
        return Paging.builder()
                .total((int) total)
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext((long) pageNumber * pageSize < total)
                .build();
    }

    private Sort getSort(GetItemsRequestDto request) {
        if (request.getSort() == null) return Sort.unsorted();
        return switch (request.getSort()) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };
    }

    private ItemDto obtainEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", null, 0);
    }

    private CartDto obtainCartDto() {
        return new CartDto(-1L, List.of(), BigDecimal.ZERO);
    }

    private List<ItemDto> flattenItems(List<List<ItemDto>> rows) {
        List<ItemDto> flat = new ArrayList<>();
        rows.forEach(flat::addAll);
        return flat;
    }
}
