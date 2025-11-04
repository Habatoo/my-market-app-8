package io.github.habatoo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity-класс для корзины покупателя в интернет-магазине.
 * Хранит идентификатор, список позиций корзины и итоговую сумму.
 * Связь с CartItem — один ко многим, каскад всех операций и удаление "осиротевших" позиций.
 */
@Data
@Entity
@Table(name = "carts")
public class Cart {
    /** Идентификатор корзины (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    /**
     * Итоговая сумма товаров в корзине (с учётом текущих цен и количества).
     */
    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;
}
