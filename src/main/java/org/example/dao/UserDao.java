package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) для работы с пользователями в базе данных.
 * Обеспечивает CRUD-операции и дополнительные методы для управления пользователями.
 */
public class UserDao {
    private static final Logger logger = Logger.getLogger(UserDao.class.getName());

    public UserDao() {
        initializeTable();
    }

    /**
     * Создает таблицу users при инициализации DAO.
     * Структура таблицы:
     * - id: первичный ключ
     * - username: уникальное имя пользователя
     * - password: хеш пароля
     * - role: роль пользователя (ADMIN/USER)
     * - email, phone, telegram_chat_id: дополнительные данные
     */
    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "role VARCHAR(10) NOT NULL, " +
                "email VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "telegram_chat_id VARCHAR(50)" +
                ")";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Таблица users инициализирована");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при инициализации таблицы users", e);
        }
    }

    /**
     * Сохраняет пользователя в базу данных
     * @param user объект пользователя для сохранения
     * @return сохраненный объект пользователя с присвоенным ID
     */
    public User save(User user) {
        String sql = "INSERT INTO users (username, password, role, email, phone, telegram_chat_id) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Установка параметров запроса
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhone());
            pstmt.setString(6, user.getTelegramChatId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getLong(1));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при сохранении пользователя", e);
        }
        return null;
    }

    /**
     * Ищет пользователя по имени
     * @param username имя пользователя для поиска
     * @return Optional с найденным пользователем или пустой
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, role, email, phone, telegram_chat_id FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setTelegramChatId(rs.getString("telegram_chat_id"));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при поиске пользователя по имени", e);
        }
        return Optional.empty();
    }

    /**
     * Ищет пользователя по ID
     * @param id идентификатор пользователя
     * @return Optional с найденным пользователем или пустой
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password, role, email, phone, telegram_chat_id FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setRole(rs.getString("role"));
                    user.setEmail(rs.getString("email"));
                    user.setPhone(rs.getString("phone"));
                    user.setTelegramChatId(rs.getString("telegram_chat_id"));
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при поиске пользователя по ID", e);
        }
        return Optional.empty();
    }

    /**
     * Получает всех пользователей, не являющихся администраторами
     * @return список обычных пользователей
     */
    public List<User> findAllNonAdmins() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password, role, email, phone, telegram_chat_id FROM users WHERE role <> 'ADMIN'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setEmail(rs.getString("email"));
                user.setPhone(rs.getString("phone"));
                user.setTelegramChatId(rs.getString("telegram_chat_id"));
                users.add(user);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при получении списка пользователей", e);
        }
        return users;
    }

    /**
     * Удаляет пользователя по ID
     * @param id идентификатор пользователя
     * @return true если удаление успешно
     */
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при удалении пользователя", e);
            return false;
        }
    }

    /**
     * Проверяет наличие администраторов в системе
     * @return true если есть хотя бы один администратор
     */
    public boolean existsAdminUser() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Ошибка при проверке наличия администраторов", e);
        }
        return false;
    }
}
