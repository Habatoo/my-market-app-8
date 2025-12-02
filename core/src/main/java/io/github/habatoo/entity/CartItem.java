package io.github.habatoo.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Позиция товара в корзине покупателя.
 * Хранит связанный товар, количество и цену на момент добавления.
 */
@Table("cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column("cart_id")
    private Long cartId;

    @Column("item_id")
    private Long itemId;

    private Integer count;

    private BigDecimal price;
}
