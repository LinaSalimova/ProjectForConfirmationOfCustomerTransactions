package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.OtpConfig;

import java.sql.*;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) для работы с конфигурацией OTP в базе данных.
 * Обеспечивает:
 * - Создание таблицы при инициализации
 * - Установку конфигурации по умолчанию
 * - Получение и обновление параметров OTP
 */
public class OtpConfigDao {
    private static final Logger logger = Logger.getLogger(OtpConfigDao.class.getName());

    public OtpConfigDao() {
        initializeTable();
        createDefaultConfigIfNotExists();
    }

    /**
     * Создает таблицу otp_config, если она не существует.
     * Структура таблицы:
     * - id: первичный ключ
     * - code_length: длина генерируемого кода
     * - lifetime_in_minutes: время жизни кода в минутах
     */
    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS otp_config (" +
                "id SERIAL PRIMARY KEY, " +
                "code_length INT NOT NULL, " +
                "lifetime_in_minutes INT NOT NULL" +
                ")";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Таблица otp_config инициализирована");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при инициализации таблицы otp_config", e);
        }
    }

    /**
     * Создает конфигурацию по умолчанию (6 символов, 5 минут),
     * если таблица пустая
     */
    private void createDefaultConfigIfNotExists() {
        String countSql = "SELECT COUNT(*) FROM otp_config";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO otp_config (code_length, lifetime_in_minutes) VALUES (6, 5)";
                stmt.execute(insertSql);
                logger.info("Создана конфигурация OTP по умолчанию");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при создании конфигурации по умолчанию", e);
        }
    }

    /**
     * Получает текущую конфигурацию OTP из базы данных
     * @return Optional с конфигурацией или пустой, если не найдена
     */
    public Optional<OtpConfig> getConfig() {
        String sql = "SELECT id, code_length, lifetime_in_minutes FROM otp_config LIMIT 1";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                OtpConfig config = new OtpConfig();
                config.setId(rs.getLong("id"));
                config.setCodeLength(rs.getInt("code_length"));
                config.setLifetimeInMinutes(rs.getInt("lifetime_in_minutes"));
                return Optional.of(config);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении конфигурации OTP", e);
        }
        return Optional.empty();
    }

    /**
     * Обновляет параметры OTP в базе данных
     * @param config новая конфигурация
     * @return true если обновление успешно, false в случае ошибки
     */
    public boolean updateConfig(OtpConfig config) {
        String sql = "UPDATE otp_config SET code_length = ?, lifetime_in_minutes = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, config.getCodeLength());
            pstmt.setInt(2, config.getLifetimeInMinutes());
            pstmt.setLong(3, config.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при обновлении конфигурации OTP", e);
            return false;
        }
    }
}
