package org.example.service.scheduler;

import org.example.dao.OtpDao;
import org.example.model.Otp;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Планировщик для автоматической проверки и пометки истёкших OTP-кодов.
 * Периодически запускает задачу, которая ищет все активные, но уже просроченные OTP,
 * и обновляет их статус на "EXPIRED".
 */
public class OtpExpirationScheduler {
    // Логгер для вывода информации и ошибок
    private static final Logger logger = Logger.getLogger(OtpExpirationScheduler.class.getName());
    // Задержка перед первым запуском (секунд)
    private static final int INITIAL_DELAY_SECONDS = 0;
    // Интервал между проверками (секунд)
    private static final int CHECK_INTERVAL_SECONDS = 60;

    // DAO для доступа к OTP-кодам
    private final OtpDao otpDao;
    // Планировщик задач (один поток)
    private final ScheduledExecutorService scheduler;

    /**
     * Конструктор с внедрением DAO.
     * @param otpDao DAO для работы с OTP-кодами
     */
    public OtpExpirationScheduler(OtpDao otpDao) {
        this.otpDao = otpDao;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Запуск планировщика.
     * Периодически вызывает метод checkExpiredCodes().
     */
    public void start() {
        scheduler.scheduleAtFixedRate(
                this::checkExpiredCodes,
                INITIAL_DELAY_SECONDS,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        logger.info("Планировщик проверки истёкших OTP-кодов запущен");
    }

    /**
     * Основная задача: найти все активные, но уже просроченные OTP-коды,
     * и пометить их как EXPIRED.
     */
    private void checkExpiredCodes() {
        try {
            logger.info("Запуск проверки истёкших OTP-кодов");
            // Получаем список всех активных, но уже истёкших OTP-кодов
            List<Otp> expiredCodes = otpDao.findExpiredActiveCodes();

            for (Otp otp : expiredCodes) {
                // Обновляем статус на "EXPIRED"
                boolean updated = otpDao.updateStatus(otp.getId(), "EXPIRED");
                if (updated) {
                    logger.info("OTP-код помечен как истёкший: " + otp.getId());
                } else {
                    logger.warning("Не удалось обновить статус OTP-кода: " + otp.getId());
                }
            }

            logger.info("Обработано истёкших OTP-кодов: " + expiredCodes.size());
        } catch (Exception e) {
            logger.severe("Ошибка при проверке истёкших OTP-кодов: " + e.getMessage());
        }
    }

    /**
     * Остановка планировщика и корректное завершение работы потока.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            // Ожидаем завершения задач, если не получилось - принудительно останавливаем
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logger.info("Планировщик проверки истёкших OTP-кодов остановлен");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.severe("Принудительная остановка планировщика проверки истёкших OTP-кодов");
        }
    }
}
