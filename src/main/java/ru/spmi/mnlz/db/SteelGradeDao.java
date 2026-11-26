package ru.spmi.mnlz.db;

import ru.spmi.mnlz.calc.SteelGrade;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Доступ к справочнику марок стали (таблица steel_grades). */
public final class SteelGradeDao {

    /** Все марки, отсортированные по названию (операция «чтение с сортировкой»). */
    public List<SteelGrade> findAll() throws SQLException {
        List<SteelGrade> list = new ArrayList<>();
        String sql = "SELECT name, carbon, lambda, a_coef, m_coef FROM steel_grades ORDER BY name";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new SteelGrade(rs.getString("name"), rs.getDouble("carbon"),
                        rs.getDouble("lambda"), rs.getDouble("a_coef"), rs.getDouble("m_coef")));
            }
        }
        return list;
    }

    /** Поиск марки по подстроке названия (операция «поиск»). */
    public List<SteelGrade> findByName(String part) throws SQLException {
        List<SteelGrade> list = new ArrayList<>();
        String sql = "SELECT name, carbon, lambda, a_coef, m_coef FROM steel_grades "
                + "WHERE lower(name) LIKE ? ORDER BY name";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, "%" + part.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SteelGrade(rs.getString("name"), rs.getDouble("carbon"),
                            rs.getDouble("lambda"), rs.getDouble("a_coef"), rs.getDouble("m_coef")));
                }
            }
        }
        return list;
    }

    /** Добавление новой марки (операция «ввод»). */
    public void insert(SteelGrade g) throws SQLException {
        String sql = "INSERT INTO steel_grades(name, carbon, lambda, a_coef, m_coef) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, g.name());
            ps.setDouble(2, g.carbonPercent());
            ps.setDouble(3, g.lambdaM());
            ps.setDouble(4, g.aHeat());
            ps.setDouble(5, g.mHeat());
            ps.executeUpdate();
        }
    }

    /** Удаление марки по названию (операция «удаление»). */
    public void delete(String name) throws SQLException {
        try (PreparedStatement ps = Database.get()
                .prepareStatement("DELETE FROM steel_grades WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }
}
