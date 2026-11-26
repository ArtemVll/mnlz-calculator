package ru.spmi.mnlz.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;

/** Управление соединением SQLite: схема, первичное наполнение из ресурса. */
public final class Database {

    private static final Path DB_PATH = Path.of("data", "app.db");
    private static Connection connection;

    private Database() { }

    /** Возвращает открытое соединение, при первом вызове создаёт схему и данные. */
    public static synchronized Connection get() throws SQLException {
        if (connection != null && !connection.isClosed()) return connection;
        try {
            Files.createDirectories(DB_PATH.getParent());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Не удалось создать папку data/ для базы", e);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        createSchema(connection);
        seedIfEmpty(connection);
        return connection;
    }

    private static void createSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    role TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS steel_grades (
                    id INTEGER PRIMARY KEY,
                    name TEXT UNIQUE NOT NULL,
                    carbon REAL NOT NULL,
                    lambda REAL, a_coef REAL, m_coef REAL
                );
                CREATE TABLE IF NOT EXISTS equipment (
                    id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    machine_type TEXT,
                    radius_m REAL, streams INTEGER, ladle_capacity_t INTEGER
                );
                CREATE TABLE IF NOT EXISTS calculations (
                    id INTEGER PRIMARY KEY,
                    user_id INTEGER REFERENCES users(id),
                    carbon REAL, width_m REAL, thickness_m REAL,
                    speed_mpm REAL, cryst_len_m REAL, t_out_c REAL,
                    created TEXT
                );
                CREATE TABLE IF NOT EXISTS calc_inputs (
                    id INTEGER PRIMARY KEY,
                    calculation_id INTEGER REFERENCES calculations(id),
                    param_id INTEGER, param_name TEXT, param_value REAL
                );
                CREATE TABLE IF NOT EXISTS calc_results (
                    id INTEGER PRIMARY KEY,
                    calculation_id INTEGER REFERENCES calculations(id),
                    param_id INTEGER, param_name TEXT, param_value REAL
                );
                """);
        }
    }

    private static void seedIfEmpty(Connection c) throws SQLException {
        boolean empty = true;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) empty = false;
        }
        if (!empty) return;
        // mnlz_seed.sql лежит в src/main/resources и попадает в jar
        try (InputStream in = Database.class.getResourceAsStream("/mnlz_seed.sql")) {
            if (in == null) throw new IllegalStateException("mnlz_seed.sql не найден в ресурсах");
            String sql;
            try {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Не удалось прочитать mnlz_seed.sql", e);
            }
            try (Statement st = c.createStatement()) {
                st.execute(sql);
            }
        } catch (java.io.IOException e) {
            // сюда попадает закрытие потока — оборачиваем, чтобы не менять подпись метода
            throw new IllegalStateException("Ошибка при чтении mnlz_seed.sql", e);
        }
    }

}
