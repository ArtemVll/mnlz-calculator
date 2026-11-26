package ru.spmi.mnlz;

import ru.spmi.mnlz.db.SteelGradeDao;
import javax.swing.*;

/** Точка входа «Калькулятора металлурга». */
public class Main {

    public static void main(String[] args) {
        // Все операции с интерфейсом — в потоке событий Swing
        SwingUtilities.invokeLater(() -> {
            try {
                ru.spmi.mnlz.db.Database.get();   // временная проверка, потом уберём

                // Временная диагностика: сколько записей в каждой таблице
                try (java.sql.Statement st = ru.spmi.mnlz.db.Database.get().createStatement();
                     java.sql.ResultSet rs = st.executeQuery(
                             "SELECT (SELECT COUNT(*) FROM users) AS u, (SELECT COUNT(*) FROM steel_grades) AS g, "
                                     + "(SELECT COUNT(*) FROM equipment) AS e, (SELECT COUNT(*) FROM calculations) AS c")) {
                    rs.next();
                    System.out.printf("users=%d, grades=%d, equipment=%d, calcs=%d%n",
                            rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4));
                }

            } catch (Exception e) {
                e.printStackTrace();   // ошибка и в консоль
                JOptionPane.showMessageDialog(null, "Ошибка БД: " + e.getMessage());
            }
            JFrame frame = new JFrame("Калькулятор металлурга");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(500, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
