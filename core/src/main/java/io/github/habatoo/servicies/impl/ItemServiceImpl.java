package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.enums.Sort;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
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

        return repository.findAll()
                .collectList()
                .map(all -> applyFilteringSortingPaging(all, request))
                .flatMap(result ->
                        cartMono.flatMap(cart ->
                                loadCountsForItems(result.itemDtos(), cart.id())
                                        .map(itemCounts -> buildItemsResponse(
                                                result.itemsRows(),
                                                cart,
                                                result.paging(),
                                                itemCounts
                                        ))
                        )
                );
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

    private record PageProcessingResult(List<List<ItemDto>> itemsRows,
                                        List<ItemDto> itemDtos,
                                        Paging paging) {
    }

    private PageProcessingResult applyFilteringSortingPaging(List<Item> all, GetItemsRequestDto request) {

        List<Item> filtered = filterItems(all, request.getSearch());
        filtered = sortItems(filtered, request.getSort());

        int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;

        List<Item> page = applyPaging(filtered, pageSize, pageNumber);
        List<ItemDto> itemDtos = mapper.toDto(page);
        List<List<ItemDto>> rows = splitByRows(itemDtos, 3);

        Paging paging = Paging.builder()
                .total(filtered.size())
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(pageNumber * pageSize < filtered.size())
                .build();

        return new PageProcessingResult(rows, itemDtos, paging);
    }

    private List<Item> filterItems(List<Item> all, String search) {
        if (search == null || search.isBlank()) {
            return all;
        }
        String lower = search.trim().toLowerCase();
        return all.stream()
                .filter(i -> i.getTitle().toLowerCase().contains(lower)
                        || (i.getDescription() != null
                        && i.getDescription().toLowerCase().contains(lower)))
                .toList();
    }

    private List<Item> sortItems(List<Item> filtered, Sort sort) {
        if (sort == null) {
            return filtered;
        }

        return switch (sort) {
            case ALPHA -> filtered.stream()
                    .sorted(Comparator.comparing(Item::getTitle))
                    .toList();
            case PRICE -> filtered.stream()
                    .sorted(Comparator.comparing(Item::getPrice))
                    .toList();
            default -> filtered;
        };
    }

    private List<Item> applyPaging(List<Item> filtered, int pageSize, int pageNumber) {
        int from = (pageNumber - 1) * pageSize;
        if (from >= filtered.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(from + pageSize, filtered.size());
        return filtered.subList(from, to);
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
