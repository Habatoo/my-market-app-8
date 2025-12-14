# payment
![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)

## О проекте

**payment** <br>
 — модуль для управления платежами и кошельком пользователя. 
Он реализует следующие функции: создание платежей с проверкой баланса, отслеживание статуса платежей (SUCCESS / FAILED), 
получение текущего баланса пользователя и интеграцию с локальными и внешними сервисами оплаты.
---

## Структура проекта
```declarative;
payment/
├── api/                  # Контроллеры и конфигурация приложения - jar
├── core/                 # Core блок с основной бизнес логикой - jar 
├── start/                # Application - @SpringBootApplication -jar
└── integrations/         # Локальные интеграции payment
```
---
## Применяемые технологии

- **Java 17** — основная платформа разработки.
- **Spring Boot / WebFlux** — реактивные REST API.
- **Project Reactor** — реактивные потоки (`Mono`, `Flux`).
- **Lombok** — генерация boilerplate кода (`@Slf4j`, `@RequiredArgsConstructor`).
- **JUnit 5 / StepVerifier** — тестирование реактивных сервисов.
- **Docker / Docker Compose** — локальное развёртывание и тестирование сервисов.
- **AtomicReference / BigDecimal** — хранение и управление балансом кошелька.

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
    name: payment
server:
  port: 8081

application:
  balance: 300.00 # начальный баланс кошелька пользователя.
```

4. **API эндпоинты::**

| Метод | URL                 | Описание                   |
| ----- | ------------------- | -------------------------- |
| POST  | `/payments/payment` | Создание платежа           |
| GET   | `/payments/balance` | Получение текущего баланса |

---