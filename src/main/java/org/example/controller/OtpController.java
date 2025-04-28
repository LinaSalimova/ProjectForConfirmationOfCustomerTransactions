package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Otp;
import org.example.model.User;
import org.example.service.AuthService;
import org.example.service.OtpService;
import org.example.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Контроллер для работы с OTP-кодами (генерация и верификация).
 * Все методы требуют авторизацию пользователя через JWT.
 */
public class OtpController implements HttpHandler {
    private static final Logger logger = Logger.getLogger(OtpController.class.getName());
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Конструктор с внедрением зависимостей
    public OtpController(OtpService otpService, JwtUtil jwtUtil) {
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Главный обработчик HTTP-запросов.
     * Проверяет авторизацию и маршрутизирует запросы по endpoint-ам.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        logger.info("Получен запрос: " + method + " " + path);

        // Проверка наличия и валидности JWT-токена
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "Требуется аутентификация");
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            sendResponse(exchange, 401, "Недействительный токен");
            return;
        }

        // Получаем имя пользователя из токена
        String username = jwtUtil.getUsername(token);

        try {
            // Генерация OTP-кода
            if (path.equals("/api/otp/generate") && method.equals("POST")) {
                handleGenerateOtp(exchange, username);
                // Проверка OTP-кода
            } else if (path.equals("/api/otp/verify") && method.equals("POST")) {
                handleVerifyOtp(exchange);
            } else {
                sendResponse(exchange, 404, "Метод не найден");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при обработке запроса", e);
            sendResponse(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Генерация нового OTP-кода для пользователя.
     * Ожидает operationId и notificationType в теле запроса.
     */
    private void handleGenerateOtp(HttpExchange exchange, String username) throws IOException {
        Map<String, Object> requestMap = readRequestBody(exchange);

        String operationId = (String) requestMap.get("operationId");
        String notificationType = (String) requestMap.get("notificationType");

        if (operationId == null || notificationType == null) {
            sendResponse(exchange, 400, "ID операции и тип уведомления обязательны");
            return;
        }

        // Получение пользователя по имени (реализуйте через AuthService)
        Optional<User> userOptional = Optional.empty(); // authService.getUserByUsername(username);
        if (!userOptional.isPresent()) {
            sendResponse(exchange, 404, "Пользователь не найден");
            return;
        }

        User user = userOptional.get();
        // Генерация OTP-кода и отправка через выбранный канал
        Otp otp = otpService.generateOtp(user, operationId, notificationType);

        if (otp != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP успешно сгенерирован");
            response.put("operationId", otp.getOperationId());
            response.put("expiresAt", otp.getExpiresAt().toString());

            // Включаем код в ответ только для режима FILE
            if ("file".equalsIgnoreCase(notificationType)) {
                response.put("code", otp.getCode());
            }

            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 500, "Не удалось сгенерировать OTP");
        }
    }

    /**
     * Проверка OTP-кода по коду и operationId.
     * Ожидает code и operationId в теле запроса.
     */
    private void handleVerifyOtp(HttpExchange exchange) throws IOException {
        Map<String, Object> requestMap = readRequestBody(exchange);

        String code = (String) requestMap.get("code");
        String operationId = (String) requestMap.get("operationId");

        if (code == null || operationId == null) {
            sendResponse(exchange, 400, "Код и ID операции обязательны");
            return;
        }

        // Проверяем OTP-код через сервис
        boolean verified = otpService.verifyOtp(code, operationId);
        if (verified) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP успешно проверен");

            sendResponse(exchange, 200, objectMapper.writeValueAsString(response));
        } else {
            sendResponse(exchange, 400, "Недействительный или истекший OTP");
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
