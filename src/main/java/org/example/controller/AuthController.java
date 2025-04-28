package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.User;
import org.example.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Контроллер аутентификации и регистрации пользователей.
 * Обрабатывает запросы на регистрацию и вход.
 */
public class AuthController implements HttpHandler {
    private static final Logger logger = Logger.getLogger(AuthController.class.getName());
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Конструктор с внедрением сервиса аутентификации.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Главный обработчик HTTP-запросов.
     * Маршрутизирует запросы по endpoint-ам /api/auth/register и /api/auth/login.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        logger.info("Получен запрос: " + method + " " + path);

        try {
            // Обработка регистрации пользователя
            if (path.equals("/api/auth/register") && method.equals("POST")) {
                handleRegister(exchange);
                // Обработка входа пользователя
            } else if (path.equals("/api/auth/login") && method.equals("POST")) {
                handleLogin(exchange);
            } else {
                // Если endpoint не найден
                sendResponse(exchange, 404, "Метод не найден");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при обработке запроса", e);
            sendResponse(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Обработка регистрации пользователя.
     * Ожидает в теле запроса: username, password, email, phone, telegramChatId, isAdmin.
     */
    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, Object> requestMap = readRequestBody(exchange);

        String username = (String) requestMap.get("username");
        String password = (String) requestMap.get("password");
        String email = (String) requestMap.get("email");
        String phone = (String) requestMap.get("phone");
        String telegramChatId = (String) requestMap.get("telegramChatId");
        boolean isAdmin = Boolean.valueOf(String.valueOf(requestMap.getOrDefault("isAdmin", "false")));

        // Проверка обязательных полей
        if (username == null || password == null) {
            sendResponse(exchange, 400, "Имя пользователя и пароль обязательны");
            return;
        }

        // Регистрация пользователя через сервис
        User user = authService.register(username, password, email, phone, telegramChatId, isAdmin);
        if (user != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пользователь успешно зарегистрирован");
            response.put("username", user.getUsername());
            response.put("role", user.getRole());

            sendResponse(exchange, 201, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 400, "Не удалось зарегистрировать пользователя");
        }
    }

    /**
     * Обработка входа пользователя.
     * Ожидает в теле запроса: username, password.
     * Возвращает JWT-токен при успешной аутентификации.
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> requestMap = readRequestBody(exchange);

        String username = (String) requestMap.get("username");
        String password = (String) requestMap.get("password");

        // Проверка обязательных полей
        if (username == null || password == null) {
            sendResponse(exchange, 400, "Имя пользователя и пароль обязательны");
            return;
        }

        // Аутентификация пользователя через сервис
        String token = authService.login(username, password);
        if (token != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);

            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 401, "Неверное имя пользователя или пароль");
        }
    }

    /**
     * Вспомогательный метод для чтения тела запроса и преобразования его в Map.
     */
    private Map<String, Object> readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return objectMapper.readValue(is, Map.class);
        }
    }

    /**
     * Вспомогательный метод для отправки HTTP-ответа с указанным статусом и телом.
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
