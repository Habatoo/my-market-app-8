package io.github.habatoo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Заказ пользователя в интернет-магазине.
 * Хранит идентификатор, список заказанных товаров, итоговую сумму заказа и дату оформления.
 */
@Data
@Entity
@Table(name = "orders")
public class Order {
    /** Идентификатор заказа (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Список позиций в заказе. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    /** Итоговая сумма заказа. */
    @Column(nullable = false)
    private BigDecimal totalSum;

    /** Дата и время оформления заказа. */
    @Column(nullable = false)
    private LocalDateTime dateTime;
}
