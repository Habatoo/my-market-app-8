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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final ItemRepository repository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final ItemMapper mapper;
    private final ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate;
    private final ReactiveRedisTemplate<String, List<ItemDto>> itemsListRedisTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemsDtoResponse> getItems(GetItemsRequestDto request) {
        Mono<CartDto> cartMono = cartService.getItemsInTheCart();

        int pageSize = Optional.ofNullable(request.getPageSize()).orElse(5);
        int pageNumber = Optional.ofNullable(request.getPageNumber()).orElse(1);
        String rawSearch = Optional.ofNullable(request.getSearch()).orElse("").trim();
        Sort sort = getSort(request);

        String cacheKey = "items:list:" + rawSearch + ":" + pageSize + ":" + pageNumber + ":" + sort;

        return itemsListRedisTemplate.opsForValue()
                .get(cacheKey)
                .flatMap(cachedItems -> cartMono
                        .flatMap(cart -> loadCountsForItems(cachedItems, cart.id())
                                .map(countMap -> buildItemsResponse(
                                        splitByRows(cachedItems, 3), cart,
                                        getPaging(pageSize, pageNumber, cachedItems.size()), countMap))
                        ))
                .switchIfEmpty(
                        loadItemsFromDb(rawSearch, pageSize, pageNumber, sort, cartMono)
                                .doOnNext(response -> {
                                    itemsListRedisTemplate.opsForValue()
                                            .set(cacheKey, flattenItems(response.itemsRows()), TTL)
                                            .subscribe();
                                })
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> getItem(Long id) {
        String cacheKey = "item:card:" + id;

        Mono<ItemDto> itemMono = itemRedisTemplate.opsForValue()
                .get(cacheKey)
                .switchIfEmpty(
                        repository.findById(id)
                                .switchIfEmpty(Mono.error(new IllegalStateException("Товар с id=" + id + " не найден")))
                                .map(mapper::toDto)
                                .flatMap(dto -> itemRedisTemplate.opsForValue()
                                        .set(cacheKey, dto, TTL)
                                        .thenReturn(dto))
                );

        Mono<CartDto> cartMono = cartService.getItemsInTheCart()
                .defaultIfEmpty(obtainCartDto());

        return itemMono.zipWith(cartMono)
                .flatMap(tuple -> buildItemResponse(tuple.getT1(), tuple.getT2(), id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return cartService.changeNumberOfItems(request)
                .switchIfEmpty(Mono.defer(() -> repository.findById(request.getId()).map(mapper::toDto)))
                .zipWith(cartService.getItemsInTheCart())
                .flatMap(tuple -> buildItemResponse(tuple.getT1(), tuple.getT2(), tuple.getT1().id()));
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
