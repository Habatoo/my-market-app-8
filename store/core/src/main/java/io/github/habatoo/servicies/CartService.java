package io.github.habatoo.servicies;

import io.github.habatoo.dto.request.ChangeNumberOfItemsRequestDto;
import io.github.habatoo.dto.response.CartDto;
import io.github.habatoo.dto.response.ItemDto;
import io.github.habatoo.store.payment.model.PaymentRequest;
import reactor.core.publisher.Mono;

/**
 * Интерфейс для работы с корзиной.
 * Предоставляет бизнес-логику для операций с товарами в корзине.
 */
public interface CartService {

    /**
     * Эндпоинт уменьшения/увеличения количества товара в корзине со страницы товаров в корзине
     * POST /items?id=[id]&search=[search]&sort=[sort]&pageNumber=[pageNumber]&pageSize=[pageSize]&action=[action]
     * возвращает Редирект.
     *
     * @param request запрос на изменение количества товара
     * @return объект с товаром ItemDto
     */
    Mono<ItemDto> changeNumberOfItems(ChangeNumberOfItemsRequestDto request);

    /**
     * Эндпоинт получения страницы со списком товаров в корзине
     * GET /cart/items
     *
     * @return объект корзины с товаром CartDto
     */
    Mono<CartDto> getItemsInTheCart();

    /**
     * Эндпоинт уменьшения/увеличения количества товара в корзине со страницы корзины
     * POST /cart/items?id=[id]&action=[action]
     *
     * @param request запрос на изменение количества товара
     * @return объект корзины с товаром CartDto
     */
    Mono<CartDto> changeNumberOfItemsFromCart(ChangeNumberOfItemsRequestDto request);


    /**
     * Проверяет возможность обработки платежа с указанной суммой.
     * <p>
     * Метод выполняет запрос текущего баланса кошелька и сравнивает его
     * с необходимой суммой платежа. В результате возвращает значение:
     * <ul>
     *     <li>true — если текущий баланс достаточен для списания;</li>
     *     <li>false — если средств недостаточно или платеж не может быть обработан.</li>
     * </ul>
     *
     * @param request объект запроса на проведение платежа, содержащий сумму списания
     * @return реактивный результат, содержащий признак доступности платежа;
     * завершится ошибкой при недоступности сервиса получения баланса
     */
    Mono<Boolean> canProcessPayment(PaymentRequest request);
}
