package io.github.habatoo.controllers;

import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.servicies.impl.ItemServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemServiceImpl itemService;

    @GetMapping("/items")
    public List<ItemDto> getItems(GetItemsRequestDto request) {
        return itemService.getItems(request);
    }

    @GetMapping("/items/{id}")
    public ItemDto getItem(@PathVariable Long id) {
        return itemService.getItem(id);
    }
}
