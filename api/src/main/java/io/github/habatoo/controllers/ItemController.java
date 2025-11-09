package io.github.habatoo.controllers;

import io.github.habatoo.dto.enums.Action;
import io.github.habatoo.dto.enums.Sort;
import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер витрины магазина.
 * Отвечает за обработку запросов по просмотру списка товаров, отдельной позиции,
 * а также изменению количества товаров через корзину или карту товара.
 */
@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private static final String ITEMS = "items";

    private static final String ITEM = "item";

    private final ItemService itemService;

    private final CartService cartService;

    /**
     * Получить и отобразить список товаров с возможностью поиска и сортировки, разбивкой по страницам.
     * Результаты поиска и корзина передаются на шаблон.
     *
     * @param search    строка поиска по названию/описанию
     * @param sort      способ сортировки (NO, ALPHA, PRICE)
     * @param pageSize  размер страницы
     * @param pageNumber номер текущей страницы
     * @param model     модель для передачи атрибутов во view
     * @return имя шаблона списка товаров
     */
    @GetMapping
    public String getItems(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) Sort sort,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            Model model) {
        GetItemsRequestDto req = GetItemsRequestDto.builder()
                .search(search)
                .sort(sort != null ? sort : Sort.NO)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();
        ItemsDtoResponse items = itemService.getItems(req);

        model.addAttribute("cart", items.cart());
        model.addAttribute(ITEMS, items.itemsRows());
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("sort", req.getSort());
        model.addAttribute("paging", items.paging());

        return ITEMS;
    }

    /**
     * Изменяет количество конкретного товара в корзине и выполняет редирект на витрину с сохранением фильтров.
     *
     * @param id        идентификатор товара
     * @param action    действие ('PLUS' или 'MINUS')
     * @param search    параметр поиска (для сохранения фильтра)
     * @param sort      параметр сортировки
     * @param pageSize  размер страницы
     * @param pageNumber номер страницы
     * @return redirect на витрину товаров с актуальными фильтрами
     */
    @PostMapping
    public String changeNumberOfItems(
            @RequestParam("id") Long id,
            @RequestParam("action") String action,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) Sort sort,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .action(Action.valueOf(action))
                .build();
        cartService.changeNumberOfItems(req);

        return "redirect:/items?search=" + (search == null ? "" : search)
                + "&sort=" + (sort == null ? "NO" : sort)
                + "&pageSize=" + pageSize
                + "&pageNumber=" + pageNumber;
    }

    /**
     * Отобразить страницу отдельного товара, включая количество товара в корзине.
     *
     * @param id    идентификатор товара
     * @param model модель для передачи атрибутов
     * @return имя шаблона отдельного товара
     */
    @GetMapping("/{id}")
    public String getItemPage(
            @PathVariable("id") Long id,
            Model model) {
        ItemDtoResponse item = itemService.getItem(id);
        model.addAttribute(ITEM, item.item());
        model.addAttribute("cartCount", item.cartCount());

        return ITEM;
    }

    /**
     * Изменяет количество именно из страницы позиции товара и возвращает её же с актуальными данными.
     *
     * @param id    идентификатор товара
     * @param action действие над количеством
     * @param model модель для передачи атрибутов
     * @return имя шаблона отдельного товара
     */
    @PostMapping("/{id}")
    public String changeItemFromItemPage(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
            Model model) {
        ChangeNumberOfItemsRequestDto req = ChangeNumberOfItemsRequestDto.builder()
                .id(id)
                .action(Action.valueOf(action))
                .build();
        ItemDtoResponse item = itemService.changeNumberOfItemsFromPage(req);
        model.addAttribute(ITEM, item.item());
        model.addAttribute("cartCount", item.cartCount());

        return ITEM;
    }
}
