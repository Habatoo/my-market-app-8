package io.github.habatoo.entity;

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
 * Хранит идентификатор, список заказанных товаров, итоговую сумму заказа и дату оформления.
 */
@Table("orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private Long id;

    @Column("total_sum")
    private BigDecimal totalSum;

    @Column("date_time")
    private LocalDateTime dateTime;
}
