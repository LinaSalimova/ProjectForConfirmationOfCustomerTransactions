package org.example.service;

import org.example.dao.UserDao;
import org.example.model.User;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Сервис для управления пользователями системы.
 * Обеспечивает бизнес-логику работы с пользователями поверх UserDao.
 */
public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    private final UserDao userDao;

    /**
     * Конструктор с внедрением зависимости UserDao
     * @param userDao DAO для работы с пользователями в БД
     */
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Получает список всех обычных пользователей (не администраторов)
     * @return список пользователей с ролью USER
     */
    public List<User> getAllNonAdminUsers() {
        logger.info("Получение списка пользователей, не являющихся администраторами");
        return userDao.findAllNonAdmins();
    }

    /**
     * Удаляет пользователя по идентификатору
     * @param userId ID пользователя для удаления
     * @return true если удаление успешно, false если пользователь не найден
     */
    public boolean deleteUser(Long userId) {
        logger.info("Удаление пользователя с ID: " + userId);
        return userDao.deleteById(userId);
    }

    /**
     * Находит пользователя по идентификатору
     * @param userId ID пользователя
     * @return Optional с пользователем или пустой, если не найден
     */
    public Optional<User> getUserById(Long userId) {
        logger.info("Получение пользователя с ID: " + userId);
        return userDao.findById(userId);
    }

    /**
     * Находит пользователя по имени
     * @param username имя пользователя для поиска
     * @return Optional с пользователем или пустой, если не найден
     */
    public Optional<User> getUserByUsername(String username) {
        logger.info("Получение пользователя по имени пользователя: " + username);
        return userDao.findByUsername(username);
    }
}
