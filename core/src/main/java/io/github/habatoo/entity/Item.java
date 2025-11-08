package io.github.habatoo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Товар в интернет-магазине.
 * Хранит название, описание, изображение, цену и вспомогательное поле для отображения количества.
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id) && Objects.equals(title, item.title) && Objects.equals(description, item.description) && Objects.equals(imgPath, item.imgPath) && Objects.equals(price, item.price) && Objects.equals(count, item.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, imgPath, price, count);
    }
}
