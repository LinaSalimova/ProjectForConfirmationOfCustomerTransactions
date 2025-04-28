package org.example.service.notification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для сохранения OTP-кодов в локальный файл.
 * Используется как один из каналов доставки OTP (например, для тестирования или аудита).
 */
public class FileService {
    private static final Logger logger = Logger.getLogger(FileService.class.getName());
    // Имя файла, в который будут сохраняться OTP-коды
    private static final String OTP_FILE_PATH = "otp_codes.txt";

    /**
     * Сохраняет информацию об OTP-коде в файл.
     * @param userId       идентификатор пользователя
     * @param operationId  идентификатор операции
     * @param code         OTP-код
     * @return true, если запись успешна; false - если возникла ошибка
     */
    public boolean saveCode(String userId, String operationId, String code) {
        try (FileWriter writer = new FileWriter(OTP_FILE_PATH, true)) {
            // Формируем строку с временной меткой и данными
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String entry = String.format("[%s] User ID: %s, Operation ID: %s, Code: %s%n",
                    timestamp, userId, operationId, code);

            writer.write(entry);
            logger.info("Код сохранен в файл: " + OTP_FILE_PATH);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении кода в файл", e);
            return false;
        }
    }

    /**
     * Возвращает объект файла, в котором хранятся OTP-коды.
     * @return объект File для otp_codes.txt
     */
    public File getOtpFile() {
        return new File(OTP_FILE_PATH);
    }
}
