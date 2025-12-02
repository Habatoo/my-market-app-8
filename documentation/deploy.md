## Быстрый старт

1. **Секреты**
   <br> Обеспечить наличие секртетов в `./env/.env` 
   <br> Содержимое файла `.env`
```text
SPRING_DATASOURCE_URL=r2dbc:postgresql://shop-db:5432/shop_db
SPRING_DATASOURCE_JDBC_URL=jdbc:postgresql:://shop-db:5432/shop_db
SPRING_DATASOURCE_USERNAME=shop_admin
SPRING_DATASOURCE_PASSWORD=shop_password
POSTGRES_DB=shop_db
POSTGRES_USER=shop_admin
POSTGRES_PASSWORD=shop_password
```

2. **Собрать backend**
- Через root:
```bash
./gradlew :start:bootJar
```

JAR-файл будет в ./start/build/libs
— запускайте стандартно
```bash
java -jar start/build/libs/start-1.0-SNAPSHOT.jar
```

3. **Подготовить миграции Liquibase и настроить переменные БД**
   <br> Миграции должны лежать в `./start/src/main/resources/db/changelog/`

4. **Собрать и запустить Docker-кластер**

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
docker compose logs -f app
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
