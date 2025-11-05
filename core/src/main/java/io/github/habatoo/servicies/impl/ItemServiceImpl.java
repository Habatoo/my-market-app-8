package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.servicies.ItemService;

import java.util.List;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
public class ItemServiceImpl implements ItemService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<ItemDto>> getItems(GetItemsRequestDto request) {
        return List.of();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDto getItem(Long id) {
        return null;
    }
}
