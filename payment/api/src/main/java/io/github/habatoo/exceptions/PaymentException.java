package io.github.habatoo.exceptions;

/**
 * Базовое исключение для ошибок, возникающих при выполнении операции оплаты.
 * Используется как общее родительское исключение для бизнес-ошибок сервиса платежей.
 */
public abstract class PaymentException extends RuntimeException {
    public PaymentException() {
    }

    public PaymentException(String message) {
        super(message);
    }
}
