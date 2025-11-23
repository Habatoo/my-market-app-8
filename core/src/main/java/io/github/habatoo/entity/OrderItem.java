package io.github.habatoo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Позиция товара в заказе пользователя.
 * Хранит ссылку на товар, связанный заказ, количество и цену на момент оформления.
 */
@Table("order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    private Long id;

    @Column("item_id")
    private Long itemId;

    @Column("order_id")
    private Long orderId;

    @Column("count")
    private Integer count;

    @Column("price")
    private BigDecimal price;
}
