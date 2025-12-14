# store
![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Postgres](https://img.shields.io/badge/PostgreSQL-17-informational?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)
![Redis](https://img.shields.io/badge/Redis-%23DD0031.svg?logo=redis&logoColor=blue)

## О проекте

**store** <br> — модуль для управления товарами, корзиной и заказами пользователя. 
Он реализует следующие функции: хранение каталога товаров, управление корзиной и заказами, 
кеширование часто используемых данных в Redis, 
и интеграцию с базой данных Postgres для постоянного хранения информации.

---

## Структура проекта
```declarative;
store/
├── api/                  # Контроллеры и конфигурация приложения - jar
├── core/                 # Core блок с основной бизнес логикой - jar 
├── start/                # Application - @SpringBootApplication -jar
│ └── db/changelog/       # Миграции Liquibase
└── integrations/         # Локальные интеграции store (Postgres+Redis)
```

## Применяемые технологии

- **Java 17** — основная платформа разработки.
- **Spring Boot / WebFlux** — реактивные REST API.
- **Project Reactor** — реактивные потоки (`Mono`, `Flux`).
- **PostgreSQL** — основная база данных для хранения товаров и заказов.
- **Redis** — кеширование часто используемых данных.
- **Liquibase** — управление миграциями базы данных.
- **Docker / Docker Compose** — локальное развёртывание сервисов.
- **Lombok** — генерация boilerplate кода (`@Slf4j`, `@RequiredArgsConstructor`).
- **JUnit 5 / StepVerifier** — тестирование реактивных сервисов.

## Быстрый старт

1. **Сборка и запуск приложения:**

- В модуле start:
```bash

./gradlew bootJar 
```
- Через root:
```bash

./gradlew :start:bootJar
```

2. **Запуск через Docker Compose:**

```bash

docker-compose up -d
```

3. **Конфигурация приложения (application.yml):**
```yaml
spring:
  application:
    name: store
  datasource:
    url: jdbc:postgresql://localhost:5432/store
    username: store_user
    password: store_pass
  redis:
    host: localhost
    port: 6379

server:
  port: 8082
```

4. **API эндпоинты::**


| Метод | URL | Описание |
|-------|-----|----------|
| GET   | /store/products | Получение списка товаров |
| GET   | /store/products/{id} | Получение информации о товаре |
| POST  | /store/cart/add | Добавление товара в корзину |
| POST  | /store/cart/remove | Удаление товара из корзины |
| POST  | /store/order | Создание заказа |

---



---