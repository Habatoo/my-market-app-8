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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;
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
                .map(all -> {
                    List<Item> filtered = all;
                    if (request.getSearch() != null && !request.getSearch().isBlank()) {
                        String lower = request.getSearch().trim().toLowerCase();
                        filtered = filtered.stream()
                                .filter(i -> i.getTitle().toLowerCase().contains(lower)
                                        || (i.getDescription() != null && i.getDescription().toLowerCase().contains(lower)))
                                .toList();
                    }

                    if (request.getSort() != null) {
                        switch (request.getSort()) {
                            case ALPHA -> filtered = filtered.stream().sorted(Comparator.comparing(Item::getTitle)).toList();
                            case PRICE -> filtered = filtered.stream().sorted(Comparator.comparing(Item::getPrice)).toList();
                            default -> {}
                        }
                    }

                    int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
                    int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
                    int from = (pageNumber - 1) * pageSize;
                    int to = Math.min(from + pageSize, filtered.size());
                    List<Item> page = from < filtered.size() ? filtered.subList(from, to) : Collections.emptyList();

                    List<ItemDto> itemDtos = mapper.toDto(page);
                    List<List<ItemDto>> itemsRows = splitByRows(itemDtos, 3);

                    Paging paging = Paging.builder()
                            .total(filtered.size())
                            .pageSize(pageSize)
                            .pageNumber(pageNumber)
                            .hasPrevious(pageNumber > 1)
                            .hasNext(pageNumber * pageSize < filtered.size())
                            .build();

                    return new Object[]{itemsRows, itemDtos, paging};
                })
                .flatMap(arr -> {
                    @SuppressWarnings("unchecked")
                    List<List<ItemDto>> itemsRows = (List<List<ItemDto>>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<ItemDto> itemDtos = (List<ItemDto>) arr[1];
                    Paging paging = (Paging) arr[2];

                    return cartMono.flatMap(cart -> {
                        Long cartId = cart.id();
                        // получить map itemId->count реактивно
                        return Flux.fromIterable(itemDtos)
                                .flatMap(itemDto ->
                                        cartItemRepository.findCountByCartIdAndItemId(cartId, itemDto.id())
                                                .defaultIfEmpty(0)
                                                .map(cnt -> Map.entry(itemDto.id(), cnt))
                                )
                                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                .map(itemCounts -> ItemsDtoResponse.builder()
                                        .itemsRows(itemsRows)
                                        .cart(cart)
                                        .paging(paging)
                                        .itemCounts(itemCounts)
                                        .build()
                                );
                    });
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> getItem(Long id) {
        Mono<ItemDto> itemMono = repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalStateException("Товар с id=" + id + " не найден")))
                .map(mapper::toDto);

        Mono<CartDto> cartMono = cartService.getItemsInTheCart();

        return itemMono.zipWith(cartMono)
                .flatMap(tuple -> {
                    ItemDto itemDto = tuple.getT1();
                    CartDto cart = tuple.getT2();
                    Long cartId = cart.id();
                    return cartItemRepository.findCountByCartIdAndItemId(cartId, id)
                            .defaultIfEmpty(0)
                            .map(cnt -> ItemDtoResponse.builder()
                                    .item(itemDto)
                                    .cartCount(cnt)
                                    .build());
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<ItemDtoResponse> changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return cartService.changeNumberOfItems(request)
                .zipWith(cartService.getItemsInTheCart())
                .flatMap(tuple -> {
                    ItemDto item = tuple.getT1();
                    CartDto cart = tuple.getT2();
                    Long cartId = cart.id();
                    return cartItemRepository.findCountByCartIdAndItemId(cartId, item.id())
                            .defaultIfEmpty(0)
                            .map(cnt -> ItemDtoResponse.builder()
                                    .item(item)
                                    .cartCount(cnt)
                                    .build());
                });
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        int totalRows = (int) Math.ceil((double) items.size() / rowSize);
        return IntStream.range(0, totalRows)
                .mapToObj(i -> {
                    int from = i * rowSize;
                    int to = Math.min(items.size(), (i + 1) * rowSize);
                    List<ItemDto> sub = new ArrayList<>(items.subList(from, to));
                    while (sub.size() < rowSize) {
                        sub.add(new ItemDto(-1L, "", "", "", null, 0));
                    }
                    return sub;
                })
                .collect(Collectors.toList());
    }
}
