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
 * Entity-класс для корзины покупателя в интернет-магазине.
 * Хранит идентификатор, список позиций корзины и итоговую сумму.
 * Связь с User — один к одному
 */
@Table("carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("total")
    private BigDecimal total = BigDecimal.ZERO;
}
