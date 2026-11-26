package ru.spmi.mnlz.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Одна запись истории: исходные данные и результаты одного расчёта. */
record HistoryEntry(int id, String steel, double width, double thickness,
                    double speed, String created,
                    Map<String, Double> inputs, Map<String, Double> results) { }

/** Доступ к истории расчётов (таблицы calculations / calc_inputs / calc_results). */
public final class CalculationDao {

    /** Сохранение расчёта целиком: шапка + параметры входа + параметры результата. */
    public int save(int userId, String steel, double width, double thickness, double speed,
                    Map<String, Double> inputs, Map<String, Double> results) throws SQLException {
        Connection c = Database.get();
        int id;
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO calculations(user_id, carbon, width_m, thickness_m, speed_mpm, "
                        + "cryst_len_m, t_out_c, created) VALUES (?,?,?,?,?,?,?, datetime('now','localtime'))",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setObject(2, null);                       // carbon уточним по марке на этапе GUI
            ps.setDouble(3, width);
            ps.setDouble(4, thickness);
            ps.setDouble(5, speed);
            ps.setObject(6, null);
            ps.setObject(7, null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
        }
        insertParams(c, "calc_inputs", id, inputs);
        insertParams(c, "calc_results", id, results);
        return id;
    }

    private void insertParams(Connection c, String table, int calcId, Map<String, Double> params)
            throws SQLException {
        String sql = "INSERT INTO " + table
                + "(calculation_id, param_id, param_name, param_value) VALUES (?,?,?,?)";
        int i = 1;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Map.Entry<String, Double> e : params.entrySet()) {
                ps.setInt(1, calcId);
                ps.setInt(2, i++);
                ps.setString(3, e.getKey());
                ps.setDouble(4, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Последние N расчётов с результатами — для вкладки «База данных». */
    public List<HistoryEntry> findRecent(int limit) throws SQLException {
        List<HistoryEntry> list = new ArrayList<>();
        String sql = "SELECT id, width_m, thickness_m, speed_mpm, created FROM calculations "
                + "ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    list.add(new HistoryEntry(id, "", rs.getDouble("width_m"),
                            rs.getDouble("thickness_m"), rs.getDouble("speed_mpm"),
                            rs.getString("created"),
                            loadParams("calc_inputs", id), loadParams("calc_results", id)));
                }
            }
        }
        return list;
    }

    /** Фильтрация истории по сечению заготовки (операция «фильтрация»). */
    public List<HistoryEntry> filterBySection(double width, double thickness) throws SQLException {
        List<HistoryEntry> list = new ArrayList<>();
        String sql = "SELECT id, width_m, thickness_m, speed_mpm, created FROM calculations "
                + "WHERE width_m = ? AND thickness_m = ? ORDER BY id DESC";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setDouble(1, width);
            ps.setDouble(2, thickness);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    list.add(new HistoryEntry(id, "", rs.getDouble("width_m"),
                            rs.getDouble("thickness_m"), rs.getDouble("speed_mpm"),
                            rs.getString("created"),
                            loadParams("calc_inputs", id), loadParams("calc_results", id)));
                }
            }
        }
        return list;
    }

    /** Удаление расчёта вместе с его параметрами (каскад вручную). */
    public void delete(int calcId) throws SQLException {
        Connection c = Database.get();
        try (PreparedStatement p1 = c.prepareStatement("DELETE FROM calc_inputs WHERE calculation_id = ?");
             PreparedStatement p2 = c.prepareStatement("DELETE FROM calc_results WHERE calculation_id = ?");
             PreparedStatement p3 = c.prepareStatement("DELETE FROM calculations WHERE id = ?")) {
            p1.setInt(1, calcId); p1.executeUpdate();
            p2.setInt(1, calcId); p2.executeUpdate();
            p3.setInt(1, calcId); p3.executeUpdate();
        }
    }

    private Map<String, Double> loadParams(String table, int calcId) throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT param_name, param_value FROM " + table
                + " WHERE calculation_id = ? ORDER BY param_id";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, calcId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("param_name"), rs.getDouble("param_value"));
            }
        }
        return map;
    }
}
