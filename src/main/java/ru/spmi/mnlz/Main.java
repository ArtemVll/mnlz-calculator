package ru.spmi.mnlz;

import javax.swing.*;

/** Точка входа «Калькулятора металлурга». */
public class Main {

    public static void main(String[] args) {
        // Все операции с интерфейсом — в потоке событий Swing
        SwingUtilities.invokeLater(() -> {
            try {
                ru.spmi.mnlz.db.Database.get();   // временная проверка, потом уберём
            } catch (Exception e) {
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
