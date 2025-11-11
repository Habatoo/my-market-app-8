package io.github.habatoo.utils;

import io.github.habatoo.entity.*;
import io.github.habatoo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

public abstract class BaseTest {

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected ItemRepository itemRepository;

    @BeforeEach
    void cleanUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }


    /**
     * Создать и сохранить Cart с указанной суммой.
     *
     * @param total итоговая сумма корзины
     * @return Cart с присвоенным id
     */
    protected Cart createAndSaveCart(BigDecimal total) {
        Cart cart = new Cart();
        cart.setTotal(total);
        cart.setItems(new ArrayList<>());
        return cartRepository.save(cart);
    }

    /**
     * Создать и сохранить Cart с total = 0.
     *
     * @return Новый пустой Cart
     */
    protected Cart createAndSaveCart() {
        return createAndSaveCart(BigDecimal.ZERO);
    }

    /**
     * Создать и сохранить Item с указанными параметрами.
     *
     * @param title название
     * @param price цена товара
     * @return Item с присвоенным id
     */
    protected Item createAndSaveItem(String title, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription("desc_" + title);
        item.setImgPath("img/" + title);
        item.setPrice(price);
        return itemRepository.save(item);
    }

    /**
     * Создать и сохранить CartItem для указанной корзины и товара.
     *
     * @param cart  корзина
     * @param item  товар
     * @param count количество
     * @param price цена позиции
     * @return CartItem с присвоенным id
     */
    protected CartItem createAndSaveCartItem(Cart cart, Item item, int count, BigDecimal price) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setItem(item);
        cartItem.setCount(count);
        cartItem.setPrice(price);
        return cartItemRepository.save(cartItem);
    }

    /**
     * Создаёт новый экземпляр Item с указанными параметрами.
     *
     * @param title       Название товара
     * @param description Описание товара
     * @param imgPath     Путь к изображению
     * @param price       Цена товара
     * @return объект Item
     */
    protected Item createItem(String title, String description, String imgPath, BigDecimal price) {
        Item item = new Item();
        item.setTitle(title);
        item.setDescription(description);
        item.setImgPath(imgPath);
        item.setPrice(price);
        return item;
    }

    /**
     * Сохраняет товар и возвращает его id.
     *
     * @param item экземпляр Item
     * @return id сохранённого Item
     */
    protected Long saveItem(Item item) {
        return itemRepository.save(item).getId();
    }

    /**
     * Вспомогательный метод создания и сохранения Order.
     */
    protected Order createAndSaveOrder(BigDecimal totalSum, LocalDateTime dateTime) {
        Order order = new Order();
        order.setTotalSum(totalSum);
        order.setDateTime(dateTime);
        order.setItems(new ArrayList<>());
        return orderRepository.save(order);
    }

    /**
     * Создать и сохранить OrderItem, привязав к нему заказ и товар.
     */
    protected OrderItem createAndSaveOrderItem(Order order, Item item, int count, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setItem(item);
        orderItem.setCount(count);
        orderItem.setPrice(price);
        orderItemRepository.save(orderItem);

        order.getItems().add(orderItem);
        orderRepository.save(order);

        return orderItem;
    }
}
