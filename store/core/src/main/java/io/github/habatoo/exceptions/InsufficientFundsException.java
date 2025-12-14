package io.github.habatoo.exceptions;

/**
 * Ошибка, возникающая в случае недостаточности средств для совершения платежа.
 * Выбрасывается, когда сумма списания превышает текущий баланс,
 * и операция оплаты не может быть завершена.
 */
public class InsufficientFundsException extends PaymentException {
    public InsufficientFundsException() {
        super("Недостаточно средств для совершения платежа");
    }
}
