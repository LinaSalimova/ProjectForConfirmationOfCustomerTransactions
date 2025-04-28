# OTP Service - Микросервис для управления одноразовыми паролями

![Java](https://img.shields.io/badge/Java-11-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-green)
![Docker](https://img.shields.io/badge/Docker-24.0-cyan)

Микросервис для генерации, отправки и верификации OTP через email, SMS, Telegram и файловую систему. Обеспечивает безопасную двухфакторную аутентификацию.

## Содержание
- [Возможности](#возможности)
- [Технологический стек](#технологический-стек)
- [Архитектура](#архитектура)
- [API Документация](#api-документация)
- [Установка](#установка)
- [Примеры использования](#примеры-использования)
- [Безопасность](#безопасность)


## Возможности
- Генерация OTP с настраиваемыми параметрами:
  - Длина кода (4-8 символов)
  - Время жизни (1-60 минут)
- Мультиканальная доставка:
  - **Email** через SMTP
  - **SMS** через SMPP
  - **Telegram Bot**
  - Локальный файл (для тестирования)
- JWT аутентификация
- Администрирование пользователей
- Автоматическая инвалидация просроченных кодов

## Технологический стек
| Категория       | Технологии                          |
|-----------------|-------------------------------------|
| Язык            | Java 11                             |
| База данных     | PostgreSQL 15                       |
| Веб-сервер      | Java HTTP Server                    |
| Контейнеризация | Docker, Docker Compose              |
| Тестирование    | MailHog, SMPP Simulator             |
| Утилиты         | PGAdmin, JWT, SHA-256               |

## Архитектура

- src/
- ├── config # Конфигурация БД
- ├── controller # HTTP обработчики
- ├── dao # Data Access Objects
- ├── model # Сущности данных
- ├── service # Бизнес-логика
- │ ├── notification # Сервисы доставки
- │ └── scheduler # Фоновые задачи
- └── util # Вспомогательные утилиты



## API Документация

### Аутентификация
| Метод | Endpoint           | Описание                  |
|-------|--------------------|---------------------------|
| POST  | /api/auth/register | Регистрация пользователя  |
| POST  | /api/auth/login    | Получение JWT токена      |

### OTP Операции
| Метод | Endpoint            | Параметры                          |
|-------|---------------------|------------------------------------|
| POST  | /api/otp/generate   | operationId, notificationType     |
| POST  | /api/otp/verify     | operationId, code                 |

### Администрирование
- GET /api/admin/users - Список пользователей
- DELETE /api/admin/users - Удаление пользователя
- PUT /api/admin/otp/config - Обновление настроек OTP


## Установка

### Требования
- Docker 24+
- JDK 11
- Maven 3.8+

### Docker Compose
- git clone https://github.com/LinaSalimova/ProjectForConfirmationOfCustomerTransactions.git
- cd ProjectForConfirmationOfCustomerTransactions
- docker-compose up -d --build


Порты:
- Сервис: 8082
- PostgreSQL: 5432
- PGAdmin: 8084

### Ручная сборка
- mvn clean package
- java -jar target/otp-service-jar-with-dependencies.jar



## Примеры использования

### Работа с OTP через API

| Метод    | Endpoint              | Пример запроса                                                                 |
|----------|-----------------------|--------------------------------------------------------------------------------|
| **POST** | `/api/otp/generate`   | ```
|          |                       | curl -X POST http://localhost:8082/api/otp/generate \                         |
|          |                       |   -H "Authorization: Bearer ваш_токен" \                                      |
|          |                       |   -d '{                                                                       |
|          |                       |     "operationId": "payment-123",                                            |
|          |                       |     "notificationType": "telegram"                                           |
|          |                       |   }'                                                                          |
|          |                       | ```                                                                           |
| **POST** | `/api/otp/verify`     | ```
|          |                       | curl -X POST http://localhost:8082/api/otp/verify \                           |
|          |                       |   -H "Content-Type: application/json" \                                       |
|          |                       |   -d '{                                                                       |
|          |                       |     "operationId": "payment-123",                                            |
|          |                       |     "code": "794231"                                                         |
|          |                       |   }'                                                                          |
|          |                       | ```                                                                           |

**Пример успешного ответа:**
{
"operationId": "payment-123",
"expiresAt": "2025-04-28T20:15:30",
"status": "ACTIVE"
}



## Меры безопасности

| Аспект безопасности              | Реализация                                                                 |
|-----------------------------------|----------------------------------------------------------------------------|
| Хеширование паролей              | SHA-256 с уникальной солью для каждого пользователя                       |
| Время жизни токенов              | JWT с 24-часовым сроком действия                                          |
| Защита от перебора               | Ограничение 5 попыток верификации + блокировка на 15 минут после 3 ошибок |
| Шифрование соединения            | Обязательное использование HTTPS в production-среде                      |
| Валидация входных данных         | Проверка формата всех входящих параметров                                 |
| Сессионные коды OTP              | Автоматическая инвалидация после использования или истечения времени      |
