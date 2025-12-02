# my-blog-back-app
![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Postgres](https://img.shields.io/badge/PostgreSQL-17-informational?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)
![Redis](https://img.shields.io/badge/Redis-%23DD0031.svg?logo=redis&logoColor=blue)

## О проекте

**my-market-app-7** <br>

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
│   └── integrationtests/     # Локальные интеграции store (Postgres+Redis)
├── payment/
│   ├── api/                  # Контроллеры и конфигурация приложения - jar
│   ├── core/                 # Core блок с основной бизнес логикой - jar 
│   ├── start/                # Application - @SpringBootApplication -jar
│   └── integrationtests/     # Локальные интеграции payment (Postgres)
├── integration-tests/        # Cross-service ТЕСТЫ store + payment
│   └── build.gradle
├── report/                   # Общий Jacoco coverage aggregator
│   └── build.gradle
├── documentation/
├── env/
├── docker-compose.yml         # Главный файл оркестрации Docker сервисов
├── Dockerfile
├── README.md
└── settings.gradle
```
---
## Применяемые технологии


## Быстрый старт


## Более расширенные инструкции

- [Работа с БД и миграциями Liquibase](store/documentation/database.md)
- [Руководство по деплою и настройкам](store/documentation/deploy.md)
- [Получение отчетов jacoco](store/documentation/jacoco.md)

---
