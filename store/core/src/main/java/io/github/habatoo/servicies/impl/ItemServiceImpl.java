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
import java.util.function.Supplier;
import java.util.stream.IntStream;

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
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemsDtoResponse> getItems(GetItemsRequestDto request) {
        int pageSize = Optional.ofNullable(request.getPageSize()).orElse(5);
        int pageNumber = Optional.ofNullable(request.getPageNumber()).orElse(1);
        String search = Optional.ofNullable(request.getSearch()).orElse("").trim();
        Sort sort = getSort(request);

        String cacheKey = "items:" + search + ":" + pageSize + ":" + pageNumber + ":" + sort;

        return cacheGetOrLoad(cacheKey, ItemsDtoResponse.class,
                () -> loadItemsFromDb(search, pageSize, pageNumber, sort));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> getItem(Long id) {
        Mono<ItemDto> itemMono = loadItemDto(id)
                .defaultIfEmpty(obtainEmptyItemDto());
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

    private Mono<ItemDtoResponse> buildItemResponse(ItemDto item, CartDto cart, Long itemId) {
        return getCartItemCount(cart.id(), itemId)
                .map(cnt -> ItemDtoResponse.builder()
                        .item(item)
                        .cartCount(cnt)
                        .build());
    }

    private Mono<Integer> getCartItemCount(Long cartId, Long itemId) {
        return cartItemRepository.findCountByCartIdAndItemId(cartId, itemId)
                .defaultIfEmpty(0);
    }

    private Mono<ItemDto> loadItemDto(Long id) {
        return cacheGetOrLoad("item:" + id, ItemDto.class, () ->
                repository.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalStateException("Item not found: " + id)))
                        .map(mapper::toDto)
        );
    }

    private <T> Mono<T> cacheGetOrLoad(String key, Class<T> clazz, Supplier<Mono<T>> loader) {
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .cast(clazz)
                .doOnNext(val -> log.info("CACHE HIT: {}", key))
                .switchIfEmpty(loader.get()
                        .flatMap(value -> reactiveRedisTemplate.opsForValue()
                                .set(key, value, TTL)
                                .thenReturn(value)
                        )
                );
    }

    private Mono<ItemsDtoResponse> loadItemsFromDb(String search, int pageSize, int pageNumber, Sort sort) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        Flux<Item> itemsFlux = (search.isBlank())
                ? repository.findAllBy(pageable)
                : repository.findByTitleContainingOrDescriptionContaining(search, search, pageable);

        Mono<Long> totalMono = (search.isBlank())
                ? repository.count()
                : repository.countByTitleContainingOrDescriptionContaining(search, search);

        return itemsFlux.collectList()
                .map(mapper::toDto)
                .zipWith(cartService.getItemsInTheCart())
                .flatMap(tuple -> loadCountsForItems(tuple.getT1(), tuple.getT2().id())
                        .map(countMap -> Tuples.of(tuple.getT1(), tuple.getT2(), countMap))
                )
                .zipWith(totalMono)
                .map(tuple -> {
                    List<ItemDto> items = tuple.getT1().getT1();
                    CartDto cart = tuple.getT1().getT2();
                    Map<Long, Integer> countMap = tuple.getT1().getT3();
                    long total = tuple.getT2();

                    List<List<ItemDto>> rows = splitByRows(items, 3);
                    Paging paging = getPaging(pageSize, pageNumber, total);

                    return buildItemsResponse(rows, cart, paging, countMap);
                });
    }

    private Mono<Map<Long, Integer>> loadCountsForItems(List<ItemDto> itemDtos, Long cartId) {
        return Flux.fromIterable(itemDtos)
                .flatMap(itemDto -> cartItemRepository.findCountByCartIdAndItemId(cartId, itemDto.id())
                        .defaultIfEmpty(0)
                        .map(cnt -> Map.entry(itemDto.id(), cnt)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
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
        return Optional.ofNullable(request.getSort())
                .map(sort -> switch (sort) {
                    case ALPHA -> Sort.by("title").ascending();
                    case PRICE -> Sort.by("price").ascending();
                    default -> Sort.unsorted();
                })
                .orElse(Sort.unsorted());
    }

    private ItemsDtoResponse buildItemsResponse(
            List<List<ItemDto>> rows,
            CartDto cart,
            Paging paging,
            Map<Long, Integer> itemCounts) {
        return ItemsDtoResponse.builder()
                .itemsRows(rows)
                .cart(cart)
                .paging(paging)
                .itemCounts(itemCounts)
                .build();
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        int totalRows = (int) Math.ceil((double) items.size() / rowSize);

        return IntStream.range(0, totalRows)
                .mapToObj(i -> {
                    int from = i * rowSize;
                    int to = Math.min(items.size(), (i + 1) * rowSize);
                    List<ItemDto> sub = new ArrayList<>(items.subList(from, to));

                    while (sub.size() < rowSize) {
                        sub.add(obtainEmptyItemDto());
                    }
                    return sub;
                })
                .toList();
    }

    private ItemDto obtainEmptyItemDto() {
        return new ItemDto(-1L, "", "", "", null, 0);
    }

    private CartDto obtainCartDto() {
        return new CartDto(-1L, List.of(), BigDecimal.ZERO);
    }
}
