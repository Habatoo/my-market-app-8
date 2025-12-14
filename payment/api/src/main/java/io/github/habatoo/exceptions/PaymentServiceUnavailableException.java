package io.github.habatoo.exceptions;

/**
 * Ошибка, сигнализирующая о недоступности сервиса платежей.
 * Может возникать при технических сбоях, сетевых ошибках или недоступности внешнего API.
 * Данная ошибка позволяет корректно оповестить пользователя и прервать процесс оформления заказа.
 */
public class PaymentServiceUnavailableException extends PaymentException {
    public PaymentServiceUnavailableException() {
        super("Сервис платежей недоступен");
    }
}
