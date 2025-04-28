package org.example.model;

import java.time.LocalDateTime;

/**
 * Модель одноразового пароля (OTP).
 * Содержит всю информацию о сгенерированном OTP-коде.
 */
public class Otp {
    // Уникальный идентификатор OTP-кода в базе данных
    private Long id;

    // Идентификатор пользователя, для которого сгенерирован код
    private Long userId;

    // Идентификатор операции, для которой предназначен OTP (например, "reset-password")
    private String operationId;

    // Сам OTP-код (строка из цифр)
    private String code;

    // Время создания OTP-кода
    private LocalDateTime createdAt;

    // Время истечения срока действия кода
    private LocalDateTime expiresAt;

    // Статус кода: ACTIVE (активный), EXPIRED (истёк), USED (использован)
    private String status; // ACTIVE, EXPIRED, USED

    /**
     * Конструктор без параметров (нужен для сериализации/десериализации)
     */
    public Otp() {
    }

    /**
     * Конструктор со всеми параметрами
     */
    public Otp(Long id, Long userId, String operationId, String code, LocalDateTime createdAt, LocalDateTime expiresAt, String status) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    // Геттеры и сеттеры для всех полей

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Проверяет, что статус OTP-кода - "ACTIVE"
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Проверяет, что код истёк (статус EXPIRED или время истекло)
     */
    public boolean isExpired() {
        return "EXPIRED".equals(status) || LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Проверяет, что код уже был использован (статус USED)
     */
    public boolean isUsed() {
        return "USED".equals(status);
    }
}
