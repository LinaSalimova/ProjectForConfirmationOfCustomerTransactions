package org.example.service;

import org.example.dao.OtpConfigDao;
import org.example.dao.OtpDao;
import org.example.model.Otp;
import org.example.model.OtpConfig;
import org.example.model.User;
import org.example.service.notification.EmailService;
import org.example.service.notification.FileService;
import org.example.service.notification.SmsService;
import org.example.service.notification.TelegramService;
import org.example.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Основной сервис для работы с OTP-кодами.
 * Обеспечивает полный жизненный цикл OTP:
 * - Генерация кодов с учетом конфигурации
 * - Отправка через различные каналы связи
 * - Верификация и инвалидация кодов
 * - Управление конфигурацией OTP
 */
public class OtpService {
    private static final Logger logger = Logger.getLogger(OtpService.class.getName());

    // Репозитории для работы с данными
    private final OtpDao otpDao;          // Доступ к OTP-кодам в БД
    private final OtpConfigDao otpConfigDao; // Доступ к настройкам OTP

    // Сервисы для отправки уведомлений
    private final EmailService emailService;    // Отправка по email
    private final SmsService smsService;        // Отправка по SMS
    private final TelegramService telegramService; // Отправка в Telegram
    private final FileService fileService;      // Сохранение в файл

    // Утилиты
    private final PasswordUtil passwordUtil = new PasswordUtil(); // Генерация OTP

    /**
     * Конструктор с внедрением зависимостей
     */
    public OtpService(OtpDao otpDao, OtpConfigDao otpConfigDao,
                      EmailService emailService, SmsService smsService,
                      TelegramService telegramService, FileService fileService) {
        this.otpDao = otpDao;
        this.otpConfigDao = otpConfigDao;
        this.emailService = emailService;
        this.smsService = smsService;
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    /**
     * Генерирует новый OTP-код для указанной операции и пользователя
     * @param user Пользователь, для которого генерируется код
     * @param operationId Идентификатор операции (например, "password-reset")
     * @param notificationType Канал отправки (email/sms/telegram/file)
     * @return Созданный OTP-объект или null при ошибке
     */
    public Otp generateOtp(User user, String operationId, String notificationType) {
        // Получение текущей конфигурации OTP
        Optional<OtpConfig> configOptional = otpConfigDao.getConfig();
        if (!configOptional.isPresent()) {
            logger.severe("Конфигурация OTP не найдена");
            return null;
        }

        // Параметры генерации из конфигурации
        OtpConfig config = configOptional.get();
        int codeLength = config.getCodeLength();
        int lifetimeInMinutes = config.getLifetimeInMinutes();

        // Генерация кода и временных меток
        String code = passwordUtil.generateOtp(codeLength);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(lifetimeInMinutes);

        // Создание и сохранение OTP
        Otp otp = new Otp();
        otp.setUserId(user.getId());
        otp.setOperationId(operationId);
        otp.setCode(code);
        otp.setCreatedAt(now);
        otp.setExpiresAt(expiresAt);
        otp.setStatus("ACTIVE");

        Otp savedOtp = otpDao.save(otp);
        if (savedOtp == null) {
            logger.severe("Ошибка сохранения OTP в БД");
            return null;
        }

        // Отправка кода через выбранный канал
        boolean notificationSent = sendNotification(user, notificationType, code);
        if (!notificationSent) {
            logger.warning("Сбой при отправке OTP через " + notificationType);
        }

        logger.info(String.format(
                "Сгенерирован OTP [%s] для %s (%s)",
                operationId, user.getUsername(), notificationType
        ));
        return savedOtp;
    }

    /**
     * Выбирает канал и отправляет OTP-код пользователю
     * @param user Получатель кода
     * @param notificationType Тип уведомления
     * @param code OTP-код
     * @return Результат отправки (true - успешно)
     */
    private boolean sendNotification(User user, String notificationType, String code) {
        switch (notificationType.toLowerCase()) {
            case "email":
                if (isValid(user.getEmail())) {
                    return emailService.sendCode(user.getEmail(), code);
                }
                logger.warning("Не указан email для пользователя: " + user.getUsername());
                return false;

            case "sms":
                if (isValid(user.getPhone())) {
                    return smsService.sendCode(user.getPhone(), code);
                }
                logger.warning("Не указан телефон для пользователя: " + user.getUsername());
                return false;

            case "telegram":
                if (isValid(user.getTelegramChatId())) {
                    return telegramService.sendCode(user.getTelegramChatId(), code);
                }
                logger.warning("Не указан Telegram ID для пользователя: " + user.getUsername());
                return false;

            case "file":
                return fileService.saveCode(
                        user.getId().toString(),
                        user.getUsername(),
                        code
                );

            default:
                logger.warning("Неподдерживаемый тип уведомления: " + notificationType);
                return false;
        }
    }

    /**
     * Проверяет валидность OTP-кода для указанной операции
     * @param code Введенный код
     * @param operationId Идентификатор операции
     * @return true если код действителен и не использован
     */
    public boolean verifyOtp(String code, String operationId) {
        Optional<Otp> otpOptional = otpDao.findByCodeAndOperationId(code, operationId);
        if (!otpOptional.isPresent()) {
            logger.warning("Не найден OTP для операции: " + operationId);
            return false;
        }

        Otp otp = otpOptional.get();

        // Проверка срока действия
        if (otp.isExpired()) {
            logger.info("Просроченный OTP для операции: " + operationId);
            otpDao.updateStatus(otp.getId(), "EXPIRED");
            return false;
        }

        // Проверка активности кода
        if (!otp.isActive()) {
            logger.info("Неактивный OTP для операции: " + operationId);
            return false;
        }

        // Пометить как использованный
        boolean updated = otpDao.updateStatus(otp.getId(), "USED");
        if (updated) {
            logger.info("Успешная верификация OTP для операции: " + operationId);
            return true;
        } else {
            logger.warning("Ошибка обновления статуса OTP: " + operationId);
            return false;
        }
    }

    /**
     * Обновляет параметры генерации OTP
     * @param codeLength Новая длина кода (6-8)
     * @param lifetimeInMinutes Новое время жизни (1-15)
     * @return true если обновление успешно
     */
    public boolean updateOtpConfig(int codeLength, int lifetimeInMinutes) {
        return otpConfigDao.getConfig()
                .map(config -> {
                    config.setCodeLength(codeLength);
                    config.setLifetimeInMinutes(lifetimeInMinutes);
                    return otpConfigDao.updateConfig(config);
                })
                .orElseGet(() -> {
                    logger.severe("Конфигурация OTP отсутствует");
                    return false;
                });
    }

    /**
     * Возвращает текущую конфигурацию OTP
     */
    public Optional<OtpConfig> getOtpConfig() {
        return otpConfigDao.getConfig();
    }

    /**
     * Удаляет все OTP-коды пользователя
     * @param userId Идентификатор пользователя
     */
    public boolean deleteOtpsByUserId(Long userId) {
        logger.info("Удаление OTP для пользователя ID: " + userId);
        return otpDao.deleteByUserId(userId);
    }

    /**
     * Проверяет валидность строкового значения
     */
    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
