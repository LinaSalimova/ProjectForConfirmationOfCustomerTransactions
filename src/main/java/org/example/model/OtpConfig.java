package org.example.model;

/**
 * Класс конфигурации параметров OTP.
 * Используется для хранения и передачи параметров генерации одноразовых паролей.
 */
public class OtpConfig {
    // Уникальный идентификатор конфигурации (из базы данных)
    private Long id;

    // Длина генерируемого OTP-кода (например, 6)
    private int codeLength;

    // Время жизни OTP-кода в минутах (например, 5)
    private int lifetimeInMinutes;

    /**
     * Конструктор без параметров (необходим для сериализации/десериализации)
     */
    public OtpConfig() {
    }

    /**
     * Конструктор со всеми параметрами
     * @param id уникальный идентификатор конфигурации
     * @param codeLength длина кода
     * @param lifetimeInMinutes время жизни кода в минутах
     */
    public OtpConfig(Long id, int codeLength, int lifetimeInMinutes) {
        this.id = id;
        this.codeLength = codeLength;
        this.lifetimeInMinutes = lifetimeInMinutes;
    }

    // Геттеры и сеттеры для всех полей

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public int getLifetimeInMinutes() {
        return lifetimeInMinutes;
    }

    public void setLifetimeInMinutes(int lifetimeInMinutes) {
        this.lifetimeInMinutes = lifetimeInMinutes;
    }
}
