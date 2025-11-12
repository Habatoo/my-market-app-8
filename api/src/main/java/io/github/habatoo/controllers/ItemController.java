package io.github.habatoo.controllers;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.dto.response.ItemDtoResponse;
import io.github.habatoo.dto.response.ItemsDtoResponse;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер витрины магазина.
 * Отвечает за обработку запросов по просмотру списка товаров, отдельной позиции,
 * а также изменению количества товаров через корзину или карту товара.
 */
@Slf4j
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
     * DTO связывается с параметрами запроса через @ModelAttribute.
     *
     * @param req   DTO с параметрами фильтрации, поиска и пагинации
     * @param model модель для передачи атрибутов во view
     * @return имя шаблона списка товаров
     */
    @GetMapping
    public String getItems(
            @ModelAttribute GetItemsRequestDto req,
            Model model) {
        log.info("GET /items — запрос каталога товаров, search={}, sort={}, pageSize={}, pageNumber={}",
                req.getSearch(), req.getSort(), req.getPageSize(), req.getPageNumber());

        ItemsDtoResponse items = itemService.getItems(req);

        log.debug("Получено {} товаров, всего отфильтровано: {}",
                items.itemsRows(), items.paging());
        log.trace("Paging: {}", items.paging());

        model.addAttribute("cart", items.cart());
        model.addAttribute(ITEMS, items.itemsRows());
        model.addAttribute("search", req.getSearch() == null ? "" : req.getSearch());
        model.addAttribute("sort", req.getSort());
        model.addAttribute("paging", items.paging());

        return ITEMS;
    }

    /**
     * Изменяет количество конкретного товара в корзине и выполняет редирект на витрину с сохранением фильтров.
     * DTO-запрос связывается через @ModelAttribute.
     *
     * @param req DTO с параметрами товара, действия и фильтров
     * @return redirect на витрину товаров с актуальными фильтрами
     */
    @PostMapping
    public String changeNumberOfItems(
            @ModelAttribute ChangeNumberOfItemsRequestDto req) {
        log.info("POST /items — изменение количества товара из витрины, request={}", req);

        cartService.changeNumberOfItems(req);

        String redirect = "redirect:/items?search=" + (req.getSearch() == null ? "" : req.getSearch())
                + "&sort=" + (req.getSort() == null ? "NO" : req.getSort())
                + "&pageSize=" + (req.getPageSize() == null ? 5 : req.getPageSize())
                + "&pageNumber=" + (req.getPageNumber() == null ? 1 : req.getPageNumber());
        log.info("Редирект после изменения: {}", redirect);

        return redirect;
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
        log.info("GET /items/{} — запрос страницы товара", id);
        ItemDtoResponse item = itemService.getItem(id);
        model.addAttribute(ITEM, item.item());
        model.addAttribute("cartCount", item.cartCount());

        log.debug("Получен товар: id={}, cartCount={}", id, item.cartCount());

        return ITEM;
    }

    /**
     * Изменяет количество товара из страницы позиции и возвращает её же с актуальными данными.
     * DTO-запрос связывается через @ModelAttribute.
     *
     * @param id    идентификатор товара (из @PathVariable)
     * @param req   DTO только с action (или можно сделать все параметры, если надо)
     * @param model модель для передачи атрибутов
     * @return имя шаблона отдельного товара
     */
    @PostMapping("/{id}")
    public String changeItemFromItemPage(
            @PathVariable("id") Long id,
            @ModelAttribute ChangeNumberOfItemsRequestDto req,
            Model model) {
        log.info("POST /items/{} — изменение количества товара с карточки, request={}", id, req);
        req.setId(id);
        ItemDtoResponse item = itemService.changeNumberOfItemsFromPage(req);

        model.addAttribute(ITEM, item.item());
        model.addAttribute("cartCount", item.cartCount());

        log.debug("Товар обновлен: id={}, cartCount={}", id, item.cartCount());

        return ITEM;
    }
}
