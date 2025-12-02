package io.github.habatoo.controllers;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.request.GetItemsRequestDto;
import io.github.habatoo.servicies.CartService;
import io.github.habatoo.servicies.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<String> getItems(
            @ModelAttribute GetItemsRequestDto req,
            Model model
    ) {
        log.info("GET /items — запрос каталога товаров, search={}, sort={}, pageSize={}, pageNumber={}",
                req.getSearch(), req.getSort(), req.getPageSize(), req.getPageNumber());

        return itemService.getItems(req)
                .map(items -> {
                    log.debug("Получено {} строк товаров, paging={}",
                            items.itemsRows().size(), items.paging());

                    model.addAttribute("cart", items.cart());
                    model.addAttribute(ITEMS, items.itemsRows());
                    model.addAttribute("search", req.getSearch() == null ? "" : req.getSearch());
                    model.addAttribute("sort", req.getSort());
                    model.addAttribute("paging", items.paging());
                    model.addAttribute("itemCounts", items.itemCounts());

                    return ITEMS;
                });
    }

    /**
     * Изменяет количество конкретного товара в корзине и выполняет редирект на витрину с сохранением фильтров.
     * DTO-запрос связывается через @ModelAttribute.
     *
     * @param req DTO с параметрами товара, действия и фильтров
     * @return redirect на витрину товаров с актуальными фильтрами
     */
    @PostMapping
    public Mono<String> changeNumberOfItems(
            @ModelAttribute ChangeNumberOfItemsRequestDto req,
            BindingResult bindingResult) {
        log.info("POST /items — изменение количества товара из витрины, request={}", req);

        if (bindingResult.hasErrors()) {
            log.warn("Ошибка валидации DTO");

            return Mono.error(new IllegalArgumentException("Некорректные параметры изменения товара"));
        }

        String redirect = "redirect:/items?search=" + (req.getSearch() == null ? "" : req.getSearch())
                + "&sort=" + (req.getSort() == null ? "NO" : req.getSort())
                + "&pageSize=" + (req.getPageSize() == null ? 5 : req.getPageSize())
                + "&pageNumber=" + (req.getPageNumber() == null ? 1 : req.getPageNumber());

        log.info("Редирект после изменения: {}", redirect);

        return cartService.changeNumberOfItems(req)
                .thenReturn(redirect);
    }

    /**
     * Отобразить страницу отдельного товара, включая количество товара в корзине.
     *
     * @param id    идентификатор товара
     * @param model модель для передачи атрибутов
     * @return имя шаблона отдельного товара
     */
    @GetMapping("/{id}")
    public Mono<String> getItemPage(
            @PathVariable("id") Long id,
            Model model) {
        log.info("GET /items/{} — запрос страницы товара", id);

        return itemService.getItem(id)
                .doOnNext(item -> {
                    model.addAttribute(ITEM, item.item());
                    model.addAttribute("cartCount", item.cartCount());
                })
                .thenReturn(ITEM);
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
    public Mono<String> changeItemFromItemPage(
            @PathVariable("id") Long id,
            @ModelAttribute ChangeNumberOfItemsRequestDto req,
            Model model) {
        log.info("POST /items/{} — изменение количества товара с карточки, request={}", id, req);
        req.setId(id);

        return itemService.changeNumberOfItemsFromPage(req)
                .doOnNext(item -> model.addAttribute(ITEM, item.item()))
                .doOnNext(item -> model.addAttribute("cartCount", item.cartCount()))
                .thenReturn(ITEM);
    }
}
