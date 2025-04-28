package org.example.model;

/**
 * Модель пользователя системы.
 * Используется для хранения и передачи информации о пользователе.
 */
public class User {
    // Уникальный идентификатор пользователя (из базы данных)
    private Long id;

    // Имя пользователя (уникальное)
    private String username;

    // Хеш пароля пользователя
    private String password;

    // Роль пользователя (например, "ADMIN" или "USER")
    private String role;

    // Email пользователя (может быть null)
    private String email;

    // Телефон пользователя (может быть null)
    private String phone;

    // Telegram Chat ID пользователя (может быть null)
    private String telegramChatId;

    /**
     * Конструктор без параметров (нужен для сериализации/десериализации)
     */
    public User() {
    }

    /**
     * Конструктор со всеми параметрами
     * @param id идентификатор пользователя
     * @param username имя пользователя
     * @param password хеш пароля
     * @param role роль ("ADMIN" или "USER")
     * @param email email пользователя
     * @param phone телефон пользователя
     * @param telegramChatId Telegram chat ID пользователя
     */
    public User(Long id, String username, String password, String role, String email, String phone, String telegramChatId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.telegramChatId = telegramChatId;
    }

    // Геттеры и сеттеры для всех полей

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    /**
     * Проверяет, является ли пользователь администратором
     * @return true, если роль пользователя - "ADMIN"
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
