package org.example.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Утилита для работы с паролями и OTP-кодами.
 * Обеспечивает:
 * - Хеширование паролей с использованием SHA-256
 * - Верификацию паролей
 * - Генерацию случайных OTP-кодов
 */
public class PasswordUtil {
    private static final Logger logger = Logger.getLogger(PasswordUtil.class.getName());

    // Внимание: в production-среде используйте уникальную соль для каждого пользователя
    private static final String SALT = "OtpServiceSalt";

    /**
     * Хеширует пароль с использованием SHA-256 и статической соли
     * @param password пароль в открытом виде
     * @return хеш пароля в Base64 кодировке
     * @throws RuntimeException если алгоритм SHA-256 недоступен
     */
    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(SALT.getBytes()); // Добавление соли перед хешированием
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "SHA-256 алгоритм не доступен", e);
            throw new RuntimeException("Ошибка при хешировании пароля", e);
        }
    }

    /**
     * Проверяет соответствие пароля хешу
     * @param password пароль в открытом виде
     * @param hashedPassword хеш для сравнения
     * @return true если пароль совпадает с хешем
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        String newHashedPassword = hashPassword(password);
        return newHashedPassword.equals(hashedPassword);
    }

    /**
     * Генерирует случайный цифровой OTP-код заданной длины
     * @param length длина кода (6-8 символов)
     * @return строка с цифровым кодом
     */
    public String generateOtp(int length) {
        SecureRandom random = new SecureRandom(); // Криптографически безопасный генератор
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10)); // Генерация цифр от 0 до 9
        }
        return otp.toString();
    }
}
