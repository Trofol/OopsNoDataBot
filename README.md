# OopsNoDataBot

Telegram бот с интеграцией AI модели через OpenAI-совместимый API.

## Требования

- Java 21+
- Maven 3.6+
- Telegram Bot Token (получите у [@BotFather](https://t.me/BotFather))
- Доступ к OpenAI-совместимому API (например, Ollama, DeepSeek, etc.)

## Установка

1. Клонируйте репозиторий:
```bash
git clone https://github.com/yourusername/OopsNoDataBot.git
cd OopsNoDataBot
```

2. Создайте файл конфигурации:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

3. Отредактируйте `src/main/resources/application.properties` и укажите:
   - `bot.token` - токен вашего Telegram бота
   - `bot.username` - имя вашего бота
   - `ai.url` - URL для AI API
   - `ai.model` - название модели

## Запуск

```bash
mvn spring-boot:run
```

Или соберите JAR файл:
```bash
mvn clean package
java -jar target/OopsNoDataBot-0.0.1-SNAPSHOT.jar
```

## Конфигурация

Все настройки находятся в файле `application.properties`:

- **Telegram Bot**: `bot.token`, `bot.username`
- **AI Model**: `ai.url`, `ai.model`, `ai.temperature`, `ai.max-tokens`

## Особенности

- Удаление reasoning тегов из ответа AI
- Настраиваемые параметры модели
- Обработка ошибок и таймауты
- Чистое логирование

## Лицензия

MIT

