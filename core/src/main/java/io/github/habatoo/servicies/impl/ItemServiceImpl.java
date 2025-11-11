package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.*;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
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
        List<Item> all = repository.findAll();

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
                case ALPHA -> filtered = filtered.stream()
                        .sorted(java.util.Comparator.comparing(Item::getTitle)).toList();
                case PRICE -> filtered = filtered.stream()
                        .sorted(java.util.Comparator.comparing(Item::getPrice)).toList();
            }
        }

        int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
        int from = (pageNumber - 1) * pageSize;
        int to = Math.min(from + pageSize, filtered.size());

        List<Item> page = from < filtered.size() ? filtered.subList(from, to) : Collections.emptyList();

        List<ItemDto> items = mapper.toDto(page);
        List<List<ItemDto>> itemsRows = splitByRows(items, 3);
        CartDto cart = obtainCart();
        Paging paging = Paging.builder()
                .total(filtered.size())
                .pageSize(pageSize)
                .pageNumber(pageNumber)
                .hasPrevious(pageNumber > 1)
                .hasNext(pageNumber * pageSize < filtered.size())
                .build();

        return ItemsDtoResponse.builder()
                .itemsRows(itemsRows)
                .cart(cart)
                .paging(paging)
                .build();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDtoResponse getItem(Long id) {
        ItemDto item = repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new IllegalStateException("Товар с id=%d не найден".formatted(id)));
        CartDto cart = obtainCart();
        Integer cartCount = cart.getCountByItemId(id);

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
        ItemDto item = cartService.changeNumberOfItems(request);
        CartDto cart = obtainCart();
        Integer cartCount = cart.getCountByItemId(request.getId());

        return ItemDtoResponse.builder()
                .item(item)
                .cartCount(cartCount)
                .build();
    }

    private List<List<ItemDto>> splitByRows(List<ItemDto> items, int rowSize) {
        int totalRows = (int) Math.ceil((double) items.size() / rowSize);

        return IntStream.range(0, totalRows)
                .mapToObj(i -> getSubList(items, rowSize, i))
                .map(subList -> getDtoList(rowSize, subList))
                .collect(Collectors.toList());
    }

    private static List<ItemDto> getSubList(List<ItemDto> items, int rowSize, int i) {
        return items.subList(i * rowSize, Math.min(items.size(), (i + 1) * rowSize));
    }

    private static List<ItemDto> getDtoList(int rowSize, List<ItemDto> subList) {
        List<ItemDto> list = new ArrayList<>(subList);
        while (list.size() < rowSize) {
            list.add(new ItemDto(-1L, "", "", "", null, 0));
        }

        return list;
    }

    private CartDto obtainCart() {
        return cartService.getItemsInTheCart();
    }
}
