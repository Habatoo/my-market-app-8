package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.BaseMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.AbstractService;
import io.github.habatoo.servicies.ItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
@Service
public class ItemServiceImpl extends AbstractService<Item, ItemDto> implements ItemService {

    public ItemServiceImpl(
            ItemRepository repository,
            BaseMapper<Item, ItemDto> mapper) {
        super(repository, mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ItemDto> getItems(GetItemsRequestDto request) {
        List<Item> entities = repository.findAll();
        return entities.stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDto getItem(Long id) {
        return getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDto changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return getById(request.getId());
    }
}
