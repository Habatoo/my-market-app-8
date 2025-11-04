-- Создание таблицы корзин
CREATE TABLE IF NOT EXISTS carts (
    id BIGSERIAL PRIMARY KEY,
    total NUMERIC NOT NULL DEFAULT 0
);

-- Создание таблицы товаров
CREATE TABLE IF NOT EXISTS items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1024),
    img_path VARCHAR(255),
    price NUMERIC NOT NULL
);

-- Создание таблицы позиций корзины
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    count INTEGER NOT NULL DEFAULT 1,
    price NUMERIC NOT NULL,
    CONSTRAINT fk_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Создание таблицы заказов
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    total_sum NUMERIC NOT NULL,
    date_time TIMESTAMP NOT NULL
);

-- Создание таблицы позиций заказа
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    count INTEGER NOT NULL DEFAULT 1,
    price NUMERIC NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_order FOREIGN KEY (item_id) REFERENCES items(id)
);

-- Индекс на товар в позиции корзины
CREATE INDEX IF NOT EXISTS idx_cart_item_item ON cart_items(item_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_cart ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_order_item_item ON order_items(item_id);
CREATE INDEX IF NOT EXISTS idx_order_item_order ON order_items(order_id);
