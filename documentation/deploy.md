## Быстрый старт

1. **Секреты**
   <br> Обеспечить наличие секртетов в `./env/.env` 
   <br> Содержимое файла `.env`
```text
SPRING_R2DBC_URL=r2dbc:postgresql://shop-db:5432/shop_db
SPRING_DATASOURCE_JDBC_URL=jdbc:postgresql://shop-db:5432/shop_db
SPRING_DB_USERNAME=shop_admin
SPRING_DB_PASSWORD=shop_password

POSTGRES_DB=shop_db
POSTGRES_USER=shop_admin
POSTGRES_PASSWORD=shop_password

SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_REDIS_TTL=1
```

2. **Подготовить миграции Liquibase и настроить переменные БД**
   <br> Миграции должны лежать в `./start/src/main/resources/db/changelog/`

3. **Собрать и запустить Docker-кластер**

После успешной сборки образов и запуск с помощью
```bash
docker compose build
docker compose up -d
```
- Проверка запущенных контейнеров
```bash
docker compose ps
```
покажет статус сервисов. Убедитесь, что контейнеры my-blog-backend успешно работают.

- Управление<br>
  Для остановки всех сервисов:
```bash
docker compose down
```
  Для пересборки всех сервисов:
```bash
docker compose up -d --build
```

- Логирование
Для просмотра логов:
```bash
docker compose logs -f payment
docker compose logs -f store
```
Это поможет убедиться, что сервисы запустились без ошибок.

- Проверка БД
Чтобы проверить базу данных в контейнере PostgreSQL и структуру таблиц, сделайте следующее:
Подключитесь к контейнеру с базой данных командой:

```bash
docker exec -it shop-db psql -U shop_admin -d shop_db
```
где shop-db — имя контейнера, shop_admin — пользователь БД, shop_db — база данных.

После подключения в интерактивной оболочке psql выполните команду для просмотра всех таблиц:
```sql
\dt
```
Чтобы посмотреть структуру конкретной таблицы (например, post), выполните:
```sql
\d+ post
```
Для выхода из psql нажмите
```sql
\q 
```
и Enter.

---
