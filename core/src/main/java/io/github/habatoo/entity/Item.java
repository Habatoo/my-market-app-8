package io.github.habatoo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Товар в интернет-магазине.
 * Хранит название, описание, изображение, цену и вспомогательное поле для отображения количества.
 */
@Data
@Entity
@Table(name = "items")
public class Item {
    /** Идентификатор товара (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название товара. */
    @Column(nullable = false)
    private String title;

    /** Описание товара. */
    @Column(length = 1024)
    private String description;

    /** Путь к файлу изображения товара. */
    @Column
    private String imgPath;

    /** Цена товара. */
    @Column(nullable = false)
    private BigDecimal price;

    /** Текущее количество для отображения (не хранится в БД). */
    @Transient
    private Integer count = 0;
}
