package io.github.habatoo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Товар в интернет-магазине.
 *
 * @param id          Идентификатор товара
 * @param title       Название товара
 * @param description Описание товара
 * @param imgPath     Путь к файлу изображения товара
 * @param price       Цена товара
 * @param count       Текущее количество
 */
@Builder
public record ItemDto(
        Long id,
        String title,
        String description,
        String imgPath,
        BigDecimal price,
        Integer count
) {
}
