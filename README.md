# my-blog-back-app
![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Postgres](https://img.shields.io/badge/PostgreSQL-17-informational?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)

## О проекте

**my-blog-back-app** <br>
Бэкенд многомодульного блог-приложения на Java/Spring Boot (Gradle, Postgres, Flyway, Jacoco).
Поддержка миграций, автотестов, интеграции, docker-compose для легкой разработки и деплоя.
---

## Структура проекта
```declarative;
my-blog-back-app/           # ROOT проекта 
├── api/                    # Контроллеры и конфигурация приложения - jar
├── bom/                    # BOM с версиями для всего проекта.
├── core/                   # Core блок с основной бизнес логикой - jar 
├── documentation/          # Документация, инструкции, примеры миграций и тестирования
│ ├── database.md
│ ├── deploy.md
│ └── jacoco.md
├── env/                    # Папка для секретов и настроек
├── frontend/               # Исходный код frontend, конечное приложение - сюда копируется build фронта
├── gradle/                 # Wrapper
├── integrationtests /      # Интеграционные тесты по проекту
├── report /                # JacocoReport для генерации отчетеа jacoco в многомодульном проекте
├── service /               # Application - @SpringBootApplication
│ └── db/migrations/        # Миграции Flyway (V1__init_schema.sql)
├── Dockerfilel
├── docker-compose.yml      # Главный файл оркестрации Docker сервисов
├── .env                    # Переменные среды (НЕ храните в репозитории)
├── README.md
```
---
## Применяемые технологии

- **Java 17** (Spring Boot, Spring Data JPA)
- **PostgreSQL 17** (alpine образ)
- **Flyway** — миграции БД
- **Docker/Docker Compose** — весь стек развертывается одной командой
- **Nginx** — для фронтенда, проксирования статических файлов
- **Jacoco** — для сбора unit/integration coverage ([как смотреть отчёты](./documentation/jacoco.md))
- **Gradle** — сборка проекта, выполнение тестов
- **Swagger** — автоматическая генерация документации REST API
- **Actuator** — доступ к метрикам, нагрузка, состояние зависимостей, информация о сборке


## Быстрый старт

1. **Подготовка**
```bash
git clone -b feature/module_one_sprint_four_branch https://github.com/Habatoo/my-blog-back-app-2.git
cd my-blog-back-app
```

2. **Настройка базы Postgres**
- Параметры по умолчанию:  
  `DB_NAME=blog_db`  
  `USER=blog_admin`  
  `PASSWORD=blog_password`  
  (см. `.env` в папке env)

- Миграции хранятся здесь:  
  `service/src/main/resources/db/migrations/V1__init_schema.sql`

3. **Запуск через Docker Compose**
```bash
docker compose up --build
```
- Контейнеры: backend, frontend (NGINX), база, Flyway миграции.

4. **Сборка и деплой бэкенда вручную**
- В модуле service: 
```bash
./gradlew bootJar 
```
- Через root: 
```bash
./gradlew :service:bootJar
```

5. **Запуск/тесты**
- Юнит-тесты
```bash
./gradlew test
```
- Интеграционные тесты
```bash
./gradlew :integrationtests:test
```

6. **Отчёты Jacoco**<br>
Запуск:
```bash
./gradlew clean test jacocoTestReport
```

7. **Swagger доступен по ссылке:**<br>
`http://localhost:8080/swagger-ui/index.html`<br><br>

8. **Actuator**<br>
Доствупные эндпоинты:
`http://127.0.0.1:8080/actuator`<br>
Информация о сборке:
`http://127.0.0.1:8080/actuator/info`<br>
Состояние сервиса:
`http://127.0.0.1:8080/actuator/health`<br>
Метрики:
`http://127.0.0.1:8080/actuator/metrics`
---


## Доступы и взаимодействие сервисов
- **Frontend:**  
  http://localhost/  
  (отправляет запросы на API по http://localhost:8080/)
- **Backend:**  
  http://localhost:8080/  
  (автоматически подключается к сервису db и применяет миграции при первом запуске через Flyway)
- **База данных:**  
  `blog_db_con` (Postgres)  
  — видна внутри клстера по имени, снаружи доступен порт 5432

## Более расширенные инструкции

- [Работа с БД и миграциями Flyway](./documentation/database.md)
- [Руководство по деплою и настройкам](./documentation/deploy.md)
- [Получение отчетов jacoco](./documentation/database.md)
---
