package org.example.service.notification;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для отправки OTP-кодов через Telegram Bot API.
 * Использует настройки из файла telegram.properties.
 */
public class TelegramService {
    private static final Logger logger = Logger.getLogger(TelegramService.class.getName());
    private final String telegramApiUrl; // URL для отправки сообщений через Telegram Bot API
    private final String botToken;       // Токен Telegram-бота

    /**
     * Конструктор. Загружает настройки из файла telegram.properties.
     * Если файл не найден, используется значение по умолчанию.
     */
    public TelegramService() {
        Properties config = loadConfig();
        this.botToken = config.getProperty("telegram.bot_token", "your_bot_token");
        this.telegramApiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
    }

    /**
     * Загружает параметры из файла telegram.properties.
     * Если файл не найден, используются стандартные настройки.
     */
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("telegram.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                logger.warning("telegram.properties не найден, используем стандартные настройки");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при загрузке конфигурации Telegram", e);
        }
        return props;
    }

    /**
     * Отправляет OTP-код пользователю через Telegram.
     * @param chatId chat_id пользователя Telegram (строка)
     * @param code   OTP-код для отправки
     * @return true если сообщение отправлено успешно, иначе false
     */
    public boolean sendCode(String chatId, String code) {
        // Формируем текст сообщения
        String message = String.format("Ваш код подтверждения: %s", code);
        // Формируем URL для запроса к Telegram Bot API
        String url = String.format("%s?chat_id=%s&text=%s",
                telegramApiUrl,
                urlEncode(chatId),
                urlEncode(message));
        // Отправляем HTTP-запрос
        return sendTelegramRequest(url);
    }

    /**
     * Выполняет HTTP GET-запрос к Telegram Bot API для отправки сообщения.
     * @param url сформированный URL для отправки сообщения
     * @return true если Telegram вернул 200 OK, иначе false
     */
    private boolean sendTelegramRequest(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    logger.warning("Ошибка Telegram API. Код ответа: " + statusCode);
                    return false;
                } else {
                    logger.info("Сообщение Telegram успешно отправлено");
                    return true;
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при отправке сообщения в Telegram", e);
            return false;
        }
    }

    /**
     * Кодирует строку для безопасной передачи в URL (UTF-8).
     * @param value строка для кодирования
     * @return закодированная строка
     */
    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
