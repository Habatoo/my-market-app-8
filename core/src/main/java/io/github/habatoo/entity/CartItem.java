package io.github.habatoo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Позиция товара в корзине покупателя.
 * Хранит связанный товар, количество и цену на момент добавления.
 */
@Data
@Entity
@Table(name = "cart_items")
public class CartItem {
    /** Идентификатор позиции корзины (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Корзина, к которой принадлежит данная позиция. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /** Товар в корзине. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** Количество товара в позиции корзины. */
    @Column(nullable = false)
    private Integer count;

    /** Цена товара на момент добавления в корзину. */
    @Column(nullable = false)
    private BigDecimal price;
}
