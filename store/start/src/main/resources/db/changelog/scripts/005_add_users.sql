-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Обновление таблицы корзины

ALTER TABLE carts
ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE carts
ADD CONSTRAINT fk_cart_user
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Создание таблицы заказов

ALTER TABLE orders
ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE orders
ADD CONSTRAINT fk_order_user
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Индекс на user в позиции корзины и заказа
CREATE UNIQUE INDEX idx_cart_user ON carts(user_id);
CREATE INDEX idx_order_user ON orders(user_id);