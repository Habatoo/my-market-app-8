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
import java.time.LocalDateTime;

/**
 * Заказ пользователя в интернет-магазине.
 * Хранит идентификатор, список заказанных товаров, итоговую сумму заказа и дату оформления
 * и идентификатор пользователя.
 */
@Table("orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("total_sum")
    private BigDecimal totalSum;

    @Column("date_time")
    private LocalDateTime dateTime;
}
