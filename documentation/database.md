## Настройка базы для администраторов (PostgreSQL)

- Миграции liquibase применяются при запуске backend- или liquibase-контейнера
- Для полного удаления и повторного создания БД и пользователя:

```sql;
DROP DATABASE IF EXISTS shop_db;
DROP ROLE IF EXISTS shop_admin;
CREATE ROLE shop_admin WITH LOGIN PASSWORD 'shop_password';
CREATE DATABASE shop_db OWNER shop_admin;
GRANT ALL PRIVILEGES ON DATABASE shop_db TO shop_admin;
```

(Опционально) Полная очистка публичной схемы — если надо не удалять базу:
   -- Подключиться к базе: \c shop_db
```sql;
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```