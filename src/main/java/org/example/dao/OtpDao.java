package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Otp;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) для работы с OTP-кодами в базе данных.
 * Обеспечивает CRUD-операции и дополнительные методы для управления OTP.
 */
public class OtpDao {
    private static final Logger logger = Logger.getLogger(OtpDao.class.getName());

    public OtpDao() {
        initializeTable();
    }

    /**
     * Создает таблицу otps при инициализации DAO.
     * Структура таблицы:
     * - id: первичный ключ
     * - user_id: ссылка на пользователя
     * - operation_id: идентификатор операции
     * - code: OTP-код
     * - created_at/expires_at: временные метки
     * - status: статус кода (ACTIVE/EXPIRED/USED)
     */
    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS otps (" +
                "id SERIAL PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "operation_id VARCHAR(100) NOT NULL, " +
                "code VARCHAR(10) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL, " +
                "expires_at TIMESTAMP NOT NULL, " +
                "status VARCHAR(10) NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Таблица otps инициализирована");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при инициализации таблицы otps", e);
        }
    }

    /**
     * Сохраняет OTP-код в базу данных
     * @param otp объект OTP для сохранения
     * @return сохраненный объект OTP с присвоенным ID
     */
    public Otp save(Otp otp) {
        String sql = "INSERT INTO otps (user_id, operation_id, code, created_at, expires_at, status) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Установка параметров запроса
            pstmt.setLong(1, otp.getUserId());
            pstmt.setString(2, otp.getOperationId());
            pstmt.setString(3, otp.getCode());
            pstmt.setTimestamp(4, Timestamp.valueOf(otp.getCreatedAt()));
            pstmt.setTimestamp(5, Timestamp.valueOf(otp.getExpiresAt()));
            pstmt.setString(6, otp.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        otp.setId(rs.getLong(1));
                        return otp;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении OTP", e);
        }
        return null;
    }

    /**
     * Ищет OTP по коду и идентификатору операции
     * @param code код подтверждения
     * @param operationId идентификатор операции
     * @return Optional с найденным OTP или пустой
     */
    public Optional<Otp> findByCodeAndOperationId(String code, String operationId) {
        String sql = "SELECT id, user_id, operation_id, code, created_at, expires_at, status " +
                "FROM otps WHERE code = ? AND operation_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, operationId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Otp otp = new Otp();
                    otp.setId(rs.getLong("id"));
                    otp.setUserId(rs.getLong("user_id"));
                    otp.setOperationId(rs.getString("operation_id"));
                    otp.setCode(rs.getString("code"));
                    otp.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    otp.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    otp.setStatus(rs.getString("status"));
                    return Optional.of(otp);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при поиске OTP по коду и операции", e);
        }
        return Optional.empty();
    }

    /**
     * Обновляет статус OTP-кода
     * @param id идентификатор OTP
     * @param status новый статус (ACTIVE/EXPIRED/USED)
     * @return true если обновление успешно
     */
    public boolean updateStatus(Long id, String status) {
        String sql = "UPDATE otps SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, id);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при обновлении статуса OTP", e);
            return false;
        }
    }

    /**
     * Находит все активные OTP-коды с истекшим сроком действия
     * @return список просроченных OTP
     */
    public List<Otp> findExpiredActiveCodes() {
        List<Otp> expiredCodes = new ArrayList<>();
        String sql = "SELECT id, user_id, operation_id, code, created_at, expires_at, status " +
                "FROM otps WHERE status = 'ACTIVE' AND expires_at < ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Otp otp = new Otp();
                    otp.setId(rs.getLong("id"));
                    otp.setUserId(rs.getLong("user_id"));
                    otp.setOperationId(rs.getString("operation_id"));
                    otp.setCode(rs.getString("code"));
                    otp.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    otp.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    otp.setStatus(rs.getString("status"));
                    expiredCodes.add(otp);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при поиске просроченных OTP", e);
        }
        return expiredCodes;
    }

    /**
     * Находит все OTP-коды для указанного пользователя
     * @param userId идентификатор пользователя
     * @return список OTP пользователя
     */
    public List<Otp> findByUserId(Long userId) {
        List<Otp> userOtps = new ArrayList<>();
        String sql = "SELECT id, user_id, operation_id, code, created_at, expires_at, status " +
                "FROM otps WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Otp otp = new Otp();
                    otp.setId(rs.getLong("id"));
                    otp.setUserId(rs.getLong("user_id"));
                    otp.setOperationId(rs.getString("operation_id"));
                    otp.setCode(rs.getString("code"));
                    otp.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    otp.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    otp.setStatus(rs.getString("status"));
                    userOtps.add(otp);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при поиске OTP пользователя", e);
        }
        return userOtps;
    }

    /**
     * Удаляет все OTP-коды для указанного пользователя
     * @param userId идентификатор пользователя
     * @return true если удаление успешно
     */
    public boolean deleteByUserId(Long userId) {
        String sql = "DELETE FROM otps WHERE user_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при удалении OTP пользователя", e);
            return false;
        }
    }
}
