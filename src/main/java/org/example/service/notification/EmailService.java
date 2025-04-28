package org.example.service.notification;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для отправки OTP-кодов по электронной почте.
 * Использует JavaMail API и настройки из файла email.properties.
 */
public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    // Объект сессии для отправки писем
    private final Session session;
    // Email-адрес отправителя (берётся из конфигурации)
    private final String fromEmail;

    /**
     * Конструктор. Загружает конфигурацию и инициализирует сессию.
     */
    public EmailService() {
        Properties config = loadConfig();
        this.fromEmail = config.getProperty("email.from");

        // Создание сессии без аутентификации (auth=false)
        this.session = Session.getInstance(config);
    }

    /**
     * Загружает настройки почтового сервера из файла email.properties.
     * Если файл не найден - используются параметры по умолчанию для локального SMTP.
     */
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                logger.warning("email.properties не найден, используются параметры по умолчанию");
                // Значения по умолчанию для локального тестового SMTP-сервера
                props.put("mail.smtp.host", "localhost");
                props.put("mail.smtp.port", "25");
                props.put("mail.smtp.auth", "false");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("email.from", "noreply@yourdomain.com");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка загрузки конфигурации email", e);
        }
        return props;
    }

    /**
     * Отправляет письмо с OTP-кодом на указанный email.
     * @param toEmail email получателя
     * @param code OTP-код для отправки
     * @return true если письмо успешно отправлено, иначе false
     */
    public boolean sendCode(String toEmail, String code) {
        try {
            // Создание MIME-сообщения
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Ваш код подтверждения");
            message.setText("Код: " + code);

            // Отправка письма
            Transport.send(message);
            logger.info("Email отправлен на: " + toEmail);
            return true;
        } catch (MessagingException e) {
            logger.log(Level.SEVERE, "Ошибка отправки email: " + e.getMessage());
            return false;
        }
    }
}
