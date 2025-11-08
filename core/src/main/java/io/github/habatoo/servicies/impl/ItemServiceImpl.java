package io.github.habatoo.servicies.impl;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.entity.Item;
import io.github.habatoo.mappers.ItemMapper;
import io.github.habatoo.repositories.ItemRepository;
import io.github.habatoo.servicies.ItemService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Реализация для работы с товарами.
 * Предоставляет бизнес-логику для операций с отображением товаров на витрине.
 */
@Service
public class ItemServiceImpl implements ItemService {

    private final ItemRepository repository;
    private final ItemMapper mapper;

    public ItemServiceImpl(
            ItemRepository repository,
            ItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ItemDto> getItems(GetItemsRequestDto request) {
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
                default -> {
                }
            }
        }

        int pageSize = request.getPageSize() != null ? request.getPageSize() : 5;
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
        int from = (pageNumber - 1) * pageSize;
        int to = Math.min(from + pageSize, filtered.size());

        List<Item> page = from < filtered.size() ? filtered.subList(from, to) : Collections.emptyList();

        return mapper.toDto(page);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDto getItem(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemDto changeNumberOfItemsFromPage(ChangeNumberOfItemsRequestDto request) {
        return repository.findById(request.getId())
                .map(mapper::toDto)
                .orElse(null);
    }
}
