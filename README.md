# my-blog-back-app
![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Postgres](https://img.shields.io/badge/PostgreSQL-17-informational?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)

## О проекте

**my-market-app-6** <br>
Веб-приложение «Витрина интернет-магазина» многомодульного shop-приложения на Java/Spring Boot 
(Gradle, Postgres, Liquibase, Jacoco).
Поддержка миграций, автотестов, интеграции, docker-compose для легкой разработки и деплоя.
---

## Структура проекта
```declarative;
my-market-app/              # ROOT проекта 
├── api/                    # Контроллеры и конфигурация приложения - jar
├── bom/                    # BOM с версиями для всего проекта.
├── core/                   # Core блок с основной бизнес логикой - jar 
├── documentation/          # Документация, инструкции, примеры миграций и тестирования
│ ├── database.md
│ ├── deploy.md
│ └── jacoco.md
├── env/                    # Папка для секретов и настроек
├── gradle/                 # Wrapper
├── integrationtests /      # Интеграционные тесты по проекту
├── report /                # JacocoReport для генерации отчетеа jacoco в многомодульном проекте
├── start /                 # Application - @SpringBootApplication -jar
│ └── db/changelog/         # Миграции Liquibase
├── docker-compose.yml      # Главный файл оркестрации Docker сервисов
├── Dockerfilel
├── README.md
```
---
## Применяемые технологии

- **Java 21** (Spring Boot, Spring Data JPA и Hibernate ORM)
- **PostgreSQL 17** (alpine образ)
- **Gradle** — сборка проекта, выполнение тестов


## Быстрый старт

1. **Подготовка**
```bash
git clone -b feature/module_two_sprint_five_branch https://github.com/Habatoo/my-market-app-6.git
cd my-market-app-6
```

2. **Настройка базы Postgres**
- Параметры по умолчанию:  
  `DB_NAME=shop_db`  
  `USER=shop_admin`  
  `PASSWORD=shop_password`  
  (см. `.env` в папке env)

- Миграции хранятся здесь:
```bash
start/src/main/resources/db/changelog
```

3. **Сборка и деплой бэкенда вручную**
- В модуле start:
```bash
./gradlew bootJar 
```
- Через root:
```bash
./gradlew :start:bootJar
```

4. **Запуск/тесты**
- Юнит-тесты
```bash
./gradlew test
```
- Интеграционные тесты
```bash
./gradlew :integrationtests:test
```

## Более расширенные инструкции

- [Работа с БД и миграциями Liquibase](./documentation/database.md)
- [Руководство по деплою и настройкам](./documentation/deploy.md)
- [Получение отчетов jacoco](./documentation/jacoco.md)

---
