package io.github.habatoo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Позиция товара в заказе пользователя.
 * Хранит ссылку на товар, связанный заказ, количество и цену на момент оформления.
 */
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

    /** Общая цена товара на момент оформления заказа */
    @Transient
    public BigDecimal total;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
