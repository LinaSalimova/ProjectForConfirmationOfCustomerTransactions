package org.example.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Утилита для работы с JWT (JSON Web Tokens).
 * Обеспечивает генерацию, верификацию и парсинг токенов.
 */
public class JwtUtil {
    private static final Logger logger = Logger.getLogger(JwtUtil.class.getName());

    // Время жизни токена: 24 часа
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    // Секретный ключ для подписи токенов (генерируется автоматически)
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * Генерирует JWT-токен для пользователя
     * @param username имя пользователя
     * @param role роль пользователя
     * @return подписанный JWT-токен
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(username)               // Установка субъекта
                .claim("role", role)                // Добавление кастомного claim
                .setIssuedAt(now)                   // Время создания
                .setExpiration(expiration)          // Время истечения
                .signWith(key)                      // Подпись ключом
                .compact();                         // Генерация строки токена
    }

    /**
     * Парсит и возвращает claims из токена
     * @param token JWT-токен
     * @return объект claims с данными токена
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)                 // Установка ключа проверки
                .build()
                .parseClaimsJws(token)              // Парсинг токена
                .getBody();
    }

    /**
     * Проверяет валидность токена
     * @param token JWT-токен
     * @return true если токен валиден, false если поврежден или истек
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);         // Попытка парсинга
            return true;
        } catch (Exception e) {
            logger.warning("Недействительный JWT токен: " + e.getMessage());
            return false;
        }
    }

    /**
     * Извлекает имя пользователя из токена
     * @param token JWT-токен
     * @return имя пользователя (subject)
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * Извлекает роль пользователя из токена
     * @param token JWT-токен
     * @return роль пользователя
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get("role");
    }
}
