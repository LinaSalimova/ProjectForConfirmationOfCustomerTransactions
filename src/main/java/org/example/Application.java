package org.example;

import org.example.controller.AdminController;
import org.example.controller.AuthController;
import org.example.controller.OtpController;
import org.example.dao.OtpConfigDao;
import org.example.dao.OtpDao;
import org.example.dao.UserDao;
import org.example.service.AuthService;
import org.example.service.OtpService;
import org.example.service.UserService;
import org.example.service.notification.EmailService;
import org.example.service.notification.FileService;
import org.example.service.notification.SmsService;
import org.example.service.notification.TelegramService;
import org.example.service.scheduler.OtpExpirationScheduler;
import org.example.util.JwtUtil;
import org.example.util.PasswordUtil;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Application {
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        logger.info("Запуск приложения");

        // Инициализация DAO
        UserDao userDao = new UserDao();
        OtpDao otpDao = new OtpDao();
        OtpConfigDao otpConfigDao = new OtpConfigDao();

        // Инициализация утилит
        JwtUtil jwtUtil = new JwtUtil();
        PasswordUtil passwordUtil = new PasswordUtil();

        // Инициализация сервисов уведомлений
        EmailService emailService = new EmailService();
        SmsService smsService = new SmsService();
        TelegramService telegramService = new TelegramService();
        FileService fileService = new FileService();

        // Инициализация сервисов
        AuthService authService = new AuthService(userDao, passwordUtil, jwtUtil);
        UserService userService = new UserService(userDao);
        OtpService otpService = new OtpService(otpDao, otpConfigDao, emailService, smsService, telegramService, fileService);

        // Запуск планировщика для истечения OTP
        OtpExpirationScheduler scheduler = new OtpExpirationScheduler(otpDao);
        scheduler.start();

        // Создание контроллеров
        AuthController authController = new AuthController(authService);
        AdminController adminController = new AdminController(userService, otpService, jwtUtil);
        OtpController otpController = new OtpController(otpService, jwtUtil);

        // Запуск HTTP сервера
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Настройка контекстов для API
        server.createContext("/api/auth", authController);
        server.createContext("/api/admin", adminController);
        server.createContext("/api/otp", otpController);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        logger.info("Сервер запущен на порту " + PORT);
    }
}
