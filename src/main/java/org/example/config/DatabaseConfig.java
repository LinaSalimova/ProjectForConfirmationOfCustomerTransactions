package org.example.config;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
Конфигурация подключения к PostgreSQL
- Регистрирует драйвер БД при загрузке класса
- Предоставляет метод getConnection() для получения соединения
- Обрабатывает ошибки подключения
*/
public class DatabaseConfig {
    // Логирование ошибок
    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());

    // Параметры подключения (в реальном проекте лучше вынести в конфиг)
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/otp_db";
    private static final String JDBC_USER = "otp_user";
    private static final String JDBC_PASSWORD = "otp_password";

    // Статический блок для регистрации драйвера при инициализации класса
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "PostgreSQL JDBC драйвер не найден", e);
            throw new RuntimeException("PostgreSQL JDBC драйвер не найден", e);
        }
    }

    // Метод для получения соединения с БД
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }
}
