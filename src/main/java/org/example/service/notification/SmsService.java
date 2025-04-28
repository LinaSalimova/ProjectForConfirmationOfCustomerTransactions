package org.example.service.notification;

import org.smpp.Connection;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Сервис для отправки OTP-кодов по SMS через SMPP-протокол.
 * Использует настройки из файла sms.properties.
 */
public class SmsService {
    private static final Logger logger = Logger.getLogger(SmsService.class.getName());
    // Хост SMPP-сервера (например, localhost или внешний сервер)
    private final String host;
    // Порт SMPP-сервера (обычно 2775)
    private final int port;
    // Тип системы SMPP (например, "OTP")
    private final String systemType;
    // Имя отправителя (отображается у получателя)
    private final String sourceAddress;

    /**
     * Конструктор. Загружает конфигурацию SMPP из файла sms.properties.
     */
    public SmsService() {
        Properties config = loadConfig();
        this.host = config.getProperty("smpp.host", "localhost");
        this.port = Integer.parseInt(config.getProperty("smpp.port", "2775"));
        this.systemType = config.getProperty("smpp.system_type", "OTP");
        this.sourceAddress = config.getProperty("smpp.source_addr", "OTPService");
    }

    /**
     * Загружает параметры SMPP из файла sms.properties.
     * Если файл не найден - используются значения по умолчанию.
     */
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка загрузки конфигурации SMS", e);
        }
        return props;
    }

    /**
     * Отправляет SMS с OTP-кодом на указанный номер через SMPP.
     * @param destination номер получателя (в формате международного номера)
     * @param code OTP-код для отправки
     * @return true если отправка успешна, иначе false
     */
    public boolean sendCode(String destination, String code) {
        Connection connection = null;
        Session session = null;
        try {
            // Устанавливаем TCP/IP соединение с SMPP сервером
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);

            // Формируем запрос на привязку (bind) в режиме передатчика (transmitter)
            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(""); // SystemId и Password могут быть заданы при необходимости
            bindRequest.setPassword("");
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34);
            bindRequest.setAddressRange("");

            // Осуществляем bind к SMPP серверу
            session.bind(bindRequest);

            // Формируем и отправляем SMS
            SubmitSM submitSM = new SubmitSM();
            submitSM.setSourceAddr(sourceAddress); // Имя отправителя
            submitSM.setDestAddr(destination);     // Номер получателя
            submitSM.setShortMessage("Код: " + code);

            session.submit(submitSM);
            logger.info("SMS отправлено на номер: " + destination);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка отправки SMS: " + e.getMessage());
            return false;
        } finally {
            // Корректно закрываем сессию и соединение
            if (session != null) {
                try { session.unbind(); session.close(); } catch (Exception ignored) {}
            }
            if (connection != null) {
                try { connection.close(); } catch (Exception ignored) {}
            }
        }
    }
}
