package io.github.habatoo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Позиция товара в заказе пользователя.
 * Хранит ссылку на товар, связанный заказ, количество и цену на момент оформления.
 */
@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    /** Идентификатор позиции заказа (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Товар, добавленный в заказ. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    /** Заказ, к которому относится данная позиция. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    /** Количество единиц товара в заказе. */
    @Column(nullable = false)
    private Integer count;

    /** Цена товара на момент оформления заказа. */
    @Column(nullable = false)
    private BigDecimal price;
}
