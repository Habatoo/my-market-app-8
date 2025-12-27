# my-market-app
![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Postgres](https://img.shields.io/badge/PostgreSQL-17-informational?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)
![Redis](https://img.shields.io/badge/Redis-%23DD0031.svg?logo=redis&logoColor=blue)

## О проекте

**my-market-app-8** <br>
— микросервисное приложение для работы с интернет-магазином:
управление каталогом товаров, корзиной и заказами (`store`) и обработка платежей (`payment`).
Приложение построено с использованием реактивного подхода на Spring WebFlux,
с хранением данных в PostgreSQL и кешированием в Redis. 
В приложении реализована авторизация пользователей и межсервисная авторизация.
Для туннелирования при запросах снутри контейнеров изпользуется сервис [ngrok](https://ngrok.com).
Балансировка и проксирование трафика на разные сервисы осуществляется через Nginx.

---

## Структура проекта
```declarative;
my-market-app/
├── bom/                      # BOM с версиями для всего проекта.
├── store/
│   ├── api/                  # Контроллеры и конфигурация приложения - jar
│   ├── core/                 # Core блок с основной бизнес логикой - jar 
│   ├── start/                # Application - @SpringBootApplication -jar
│   │ └── db/changelog/       # Миграции Liquibase
│   ├── Dockerfile            # Конфигурация Docker для store
│   └── integrations/         # Локальные интеграции store (Postgres+Redis)
├── payment/
│   ├── api/                  # Контроллеры и конфигурация приложения - jar
│   ├── core/                 # Core блок с основной бизнес логикой - jar 
│   ├── start/                # Application - @SpringBootApplication -jar
│   ├── Dockerfile            # Конфигурация Docker для payment
│   └── integrations/         # Локальные интеграции payment
├── report/                   # Общий Jacoco coverage aggregator
│   └── build.gradle
├── documentation/
├── env/
├── docker-compose.yml         # Главный файл оркестрации Docker сервисов
├── README.md
└── settings.gradle
```
---
## Применяемые технологии

- **Java 17** — основная платформа разработки.
- **Spring Boot / WebFlux** — реактивные REST API.
- **Project Reactor** — реактивные потоки (`Mono`, `Flux`).
- **PostgreSQL** — основная база данных для хранения данных `store` и `payment`.
- **Redis** — кеширование часто используемых данных и корзины пользователя.
- **Liquibase** — управление миграциями базы данных.
- **Docker / Docker Compose** — локальное развёртывание сервисов.
- **Lombok** — генерация boilerplate кода (`@Slf4j`, `@RequiredArgsConstructor`).
- **JUnit 5 / StepVerifier** — тестирование реактивных сервисов.
- **Jacoco** — сбор покрытия тестов.
- **Keycloak** — сервер авторизации.

## Быстрый старт

1. **Подготовка**
- создать БД
```sql;
DROP DATABASE IF EXISTS shop_db;
DROP ROLE IF EXISTS shop_admin;
CREATE ROLE shop_admin WITH LOGIN PASSWORD 'shop_password';
CREATE DATABASE shop_db OWNER shop_admin;
GRANT ALL PRIVILEGES ON DATABASE shop_db TO shop_admin;
```

- скопировать приложение
```bash

git clone -b feature/module_two_sprint_eight_branch https://github.com/Habatoo/my-market-app-8.git
cd my-market-app-8
```

- сборка и запуск
```bash

docker buildx bake
docker compose up -d 
```

- приложение доступно по адресу https://unmanned-steelless-eliana.ngrok-free.dev/  через ngrok

- авторизация в приложении
  `LOGIN=user`   
  `PASSWORD=password`


2. **Модули приложения:**<br>
- Модуль `payment`
    - Управление платежами и кошельком пользователя.
    - Реализует создание платежей, проверку баланса, статусы SUCCESS / FAILED.
    - Интеграция с локальными сервисами и тестами.
    - Подробнее: [Документация модуля payment](./documentation/payment.md)

- Модуль `store`
    - Управление каталогом товаров, корзиной и заказами.
    - Кеширование через Redis, постоянное хранение в Postgres.
    - Миграции через Liquibase.
    - Подробнее: [Документация модуля store](./documentation/store.md)

## Более расширенные инструкции

- [Работа с БД и миграциями Liquibase](./documentation/database.md)
- [Руководство по деплою и настройкам](./documentation/deploy.md)
- [Получение отчетов jacoco](./documentation/jacoco.md)

---
