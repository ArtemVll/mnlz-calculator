package ru.spmi.mnlz.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

/** Проверка учётных данных: SHA-256 от «соль + пароль» против таблицы users. */
public final class AuthService {

    /** Сессия вошедшего пользователя: передаётся по всему GUI. */
    public record Session(int userId, String username, String role) { }

    /**
     * Вход по логину и паролю. Возвращает сессию или null, если пара неверна.
     */
    public Session login(String username, String password) throws SQLException {
        String sql = "SELECT id, password_hash, salt, role FROM users WHERE username = ?";
        try (PreparedStatement ps = ru.spmi.mnlz.db.Database.get().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;                 // логина нет
                String salt = rs.getString("salt");
                String expected = rs.getString("password_hash");
                String actual = sha256Hex(salt + password);
                if (!expected.equalsIgnoreCase(actual)) return null;  // пароль не совпал
                return new Session(rs.getInt("id"), username.trim(), rs.getString("role"));
            }
        }
    }

    /** SHA-256 строки в шестнадцатеричном виде. */
    static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен в этой JVM", e);
        }
    }
}
