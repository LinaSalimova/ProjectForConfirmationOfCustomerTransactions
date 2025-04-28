package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.OtpConfig;
import org.example.model.User;
import org.example.service.OtpService;
import org.example.service.UserService;
import org.example.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Контроллер для административных операций, требующих прав ADMIN
 * Обрабатывает запросы:
 * - Управление пользователями (просмотр/удаление)
 * - Конфигурация параметров OTP
 */
public class AdminController implements HttpHandler {
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());
    private final UserService userService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(UserService userService, OtpService otpService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Основной метод обработки входящих HTTP-запросов
     * Выполняет:
     * 1. Проверку JWT-токена и прав доступа
     * 2. Маршрутизацию запросов по endpoint-ам
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        logger.info("Получен запрос: " + method + " " + path);

        // Проверка наличия и валидности Bearer-токена
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "Требуется аутентификация");
            return;
        }

        // Верификация JWT-токена
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            sendResponse(exchange, 401, "Недействительный токен");
            return;
        }

        // Проверка роли пользователя
        String userRole = jwtUtil.getRole(token);
        if (!"ADMIN".equals(userRole)) {
            sendResponse(exchange, 403, "Доступ запрещен. Требуется роль администратора");
            return;
        }

        try {
            // Маршрутизация запросов
            if (path.equals("/api/admin/users") && method.equals("GET")) {
                handleGetUsers(exchange);
            } else if (path.equals("/api/admin/users") && method.equals("DELETE")) {
                handleDeleteUser(exchange);
            } else if (path.equals("/api/admin/otp/config") && method.equals("GET")) {
                handleGetOtpConfig(exchange);
            } else if (path.equals("/api/admin/otp/config") && method.equals("PUT")) {
                handleUpdateOtpConfig(exchange);
            } else {
                sendResponse(exchange, 404, "Метод не найден");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при обработке запроса", e);
            sendResponse(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Обработка GET-запроса для получения списка пользователей (без администраторов)
     * Возвращает JSON с массивом пользователей (пароли исключены)
     */
    private void handleGetUsers(HttpExchange exchange) throws IOException {
        List<User> users = userService.getAllNonAdminUsers();
        users.forEach(user -> user.setPassword(null)); // Исключаем пароли из ответа

        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
    }

    /**
     * Обработка DELETE-запроса для удаления пользователя
     * Требует параметр userId в теле запроса
     * Перед удалением пользователя удаляет все связанные OTP-коды
     */
    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        Map<String, Object> requestMap = readRequestBody(exchange);
        Long userId = Long.valueOf(String.valueOf(requestMap.get("userId")));

        if (userId == null) {
            sendResponse(exchange, 400, "ID пользователя обязателен");
            return;
        }

        // Каскадное удаление OTP-кодов пользователя
        otpService.deleteOtpsByUserId(userId);

        // Удаление пользователя
        boolean deleted = userService.deleteUser(userId);
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Пользователь успешно удален");
            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 404, "Пользователь не найден");
        }
    }

    /**
     * Обработка GET-запроса для получения текущей конфигурации OTP
     * Возвращает JSON с параметрами:
     * - codeLength: длина генерируемого кода
     * - lifetimeInMinutes: время жизни кода
     */
    private void handleGetOtpConfig(HttpExchange exchange) throws IOException {
        Optional<OtpConfig> configOptional = otpService.getOtpConfig();
        if (configOptional.isPresent()) {
            OtpConfig config = configOptional.get();
            Map<String, Object> response = new HashMap<>();
            response.put("codeLength", config.getCodeLength());
            response.put("lifetimeInMinutes", config.getLifetimeInMinutes());
            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 404, "Конфигурация OTP не найдена");
        }
    }

    /**
     * Обработка PUT-запроса для обновления конфигурации OTP
     * Требует параметры в теле запроса:
     * - codeLength: новая длина кода (6-8 символов)
     * - lifetimeInMinutes: новое время жизни (1-15 минут)
     */
    private void handleUpdateOtpConfig(HttpExchange exchange) throws IOException {
        Map<String, Object> requestMap = readRequestBody(exchange);
        Integer codeLength = (Integer) requestMap.get("codeLength");
        Integer lifetimeInMinutes = (Integer) requestMap.get("lifetimeInMinutes");

        if (codeLength == null || lifetimeInMinutes == null) {
            sendResponse(exchange, 400, "Длина кода и время жизни обязательны");
            return;
        }

        boolean updated = otpService.updateOtpConfig(codeLength, lifetimeInMinutes);
        if (updated) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Конфигурация OTP успешно обновлена");
            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 500, "Не удалось обновить конфигурацию OTP");
        }
    }

    // Вспомогательные методы

    /**
     * Чтение тела запроса и преобразование в Map
     */
    private Map<String, Object> readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return objectMapper.readValue(is, Map.class);
        }
    }

    /**
     * Отправка HTTP-ответа с заданным статусом и телом
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
