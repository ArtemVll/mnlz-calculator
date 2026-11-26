package ru.spmi.mnlz.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Доступ к витрине оборудования (таблица equipment). */
public final class EquipmentDao {

    /** Все машины, отсортированные по названию. */
    public List<Machine> findAll() throws SQLException {
        List<Machine> list = new ArrayList<>();
        String sql = "SELECT name, machine_type, radius_m, streams, ladle_capacity_t "
                + "FROM equipment ORDER BY name";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Machine(rs.getString("name"), rs.getString("machine_type"),
                        rs.getDouble("radius_m"), rs.getInt("streams"),
                        rs.getInt("ladle_capacity_t")));
            }
        }
        return list;
    }
}
