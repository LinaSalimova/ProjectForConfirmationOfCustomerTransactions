package org.example.service;

import org.example.dao.UserDao;
import org.example.model.User;
import org.example.util.JwtUtil;
import org.example.util.PasswordUtil;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Сервис аутентификации и регистрации пользователей.
 * Обеспечивает основные функции:
 * - Регистрация новых пользователей с валидацией данных
 * - Авторизация пользователей с выдачей JWT-токенов
 * - Проверка валидности токенов
 */
public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private final UserDao userDao;          // DAO для работы с пользователями в БД
    private final PasswordUtil passwordUtil;// Утилита для работы с паролями
    private final JwtUtil jwtUtil;          // Утилита для работы с JWT-токенами

    /**
     * Конструктор с внедрением зависимостей
     * @param userDao DAO для работы с пользователями
     * @param passwordUtil Утилита для хеширования и проверки паролей
     * @param jwtUtil Утилита для генерации и валидации токенов
     */
    public AuthService(UserDao userDao, PasswordUtil passwordUtil, JwtUtil jwtUtil) {
        this.userDao = userDao;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Регистрация нового пользователя в системе
     * @param username Логин пользователя (уникальный)
     * @param password Пароль в открытом виде
     * @param email Email пользователя
     * @param phone Номер телефона
     * @param telegramChatId Telegram Chat ID
     * @param isAdmin Флаг администратора
     * @return Зарегистрированный пользователь или null при ошибке
     */
    public User register(String username, String password, String email,
                         String phone, String telegramChatId, boolean isAdmin) {
        // Проверка уникальности логина
        Optional<User> existingUser = userDao.findByUsername(username);
        if (existingUser.isPresent()) {
            logger.warning("Попытка регистрации с существующим именем: " + username);
            return null;
        }

        // Запрет регистрации второго администратора
        if (isAdmin && userDao.existsAdminUser()) {
            logger.warning("Попытка создания второго администратора");
            return null;
        }

        // Создание и сохранение пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordUtil.hashPassword(password)); // Хеширование пароля
        user.setRole(isAdmin ? "ADMIN" : "USER");
        user.setEmail(email);
        user.setPhone(phone);
        user.setTelegramChatId(telegramChatId);

        User savedUser = userDao.save(user);
        logger.info("Успешная регистрация: " + username + " (" + user.getRole() + ")");
        return savedUser;
    }

    /**
     * Авторизация пользователя в системе
     * @param username Логин пользователя
     * @param password Пароль в открытом виде
     * @return JWT-токен или null при неудачной аутентификации
     */
    public String login(String username, String password) {
        Optional<User> userOptional = userDao.findByUsername(username);
        if (!userOptional.isPresent()) {
            logger.warning("Попытка входа несуществующего пользователя: " + username);
            return null;
        }

        User user = userOptional.get();
        if (passwordUtil.verifyPassword(password, user.getPassword())) {
            logger.info("Успешный вход: " + username);
            return jwtUtil.generateToken(username, user.getRole());
        } else {
            logger.warning("Неверный пароль для: " + username);
            return null;
        }
    }

    /**
     * Получение пользователя по логину
     * @param username Логин пользователя
     * @return Optional с пользователем или пустой
     */
    public Optional<User> getUserByUsername(String username) {
        return userDao.findByUsername(username);
    }

    /**
     * Проверка валидности JWT-токена
     * @param token JWT-токен
     * @return true если токен валиден
     */
    public boolean isTokenValid(String token) {
        return jwtUtil.validateToken(token);
    }
}
