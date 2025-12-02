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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemsDtoResponse> getItems(GetItemsRequestDto request) {
        Mono<CartDto> cartMono = cartService.getItemsInTheCart();

        int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;

        Sort sort = getSort(request);
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

        String rawSearch = request.getSearch();
        boolean noSearch = rawSearch == null || rawSearch.trim().isEmpty();

        Flux<Item> itemsFlux;
        Mono<Long> totalMono;

        if (noSearch) {
            itemsFlux = repository.findAllBy(pageable);
            totalMono = repository.count();
        } else {
            String search = rawSearch.trim();

            itemsFlux = repository.findByTitleContainingOrDescriptionContaining(search, search, pageable);
            totalMono = repository.countByTitleContainingOrDescriptionContaining(search, search);
        }

        return itemsFlux
                .collectList()
                .map(mapper::toDto)
                .zipWith(cartMono)
                .flatMap(tuple -> {
                    List<ItemDto> itemDtos = tuple.getT1();
                    CartDto cart = tuple.getT2();

                    return loadCountsForItems(itemDtos, cart.id())
                            .map(countMap -> Tuples.of(itemDtos, cart, countMap));
                })
                .zipWith(totalMono)
                .map(tuple -> {
                    List<ItemDto> items = tuple.getT1().getT1();
                    CartDto cart = tuple.getT1().getT2();
                    Map<Long, Integer> countMap = tuple.getT1().getT3();
                    Long total = tuple.getT2();

                    return getItemsDtoResponseFunction(pageSize, pageNumber, countMap)
                            .apply(Tuples.of(Tuples.of(items, cart), total));
                });
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
                .flatMap(tuple -> buildItemResponse(
                        tuple.getT1(),
                        tuple.getT2(),
                        id
                ));
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
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT1().id()));
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

    private Function<Tuple2<Tuple2<List<ItemDto>, CartDto>, Long>, ItemsDtoResponse> getItemsDtoResponseFunction(
            int pageSize, int pageNumber, Map<Long, Integer> map) {
        return tuple -> {
            CartDto cart = tuple.getT1().getT2();
            long total = tuple.getT2();
            List<ItemDto> itemDtos = tuple.getT1().getT1();
            List<List<ItemDto>> rows = splitByRows(itemDtos, 3);
            Paging paging = getPaging(pageSize, pageNumber, total);

            return buildItemsResponse(rows, cart, paging, map);
        };
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

    private Mono<ItemDtoResponse> buildItemResponse(ItemDto item, CartDto cart, Long itemId) {
        return cartItemRepository.findCountByCartIdAndItemId(cart.id(), itemId)
                .defaultIfEmpty(0)
                .map(cnt -> ItemDtoResponse.builder()
                        .item(item)
                        .cartCount(cnt)
                        .build());
    }

    private Mono<ItemDto> loadItemDto(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Товар с id=" + id + " не найден")))
                .map(mapper::toDto);
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
