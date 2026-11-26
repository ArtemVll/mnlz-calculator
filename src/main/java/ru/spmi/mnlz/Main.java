package ru.spmi.mnlz;

import ru.spmi.mnlz.ui.LoginFrame;

import javax.swing.*;

/** Точка входа «Калькулятора металлурга». */
public class Main {

    public static void main(String[] args) {
        // Все операции с интерфейсом — в потоке событий Swing
        SwingUtilities.invokeLater(() -> {
            try {
                // Гарантируем, что база создана и засеяна до первого запроса
                ru.spmi.mnlz.db.Database.get();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Ошибка БД: " + e.getMessage());
                return;
            }
            new LoginFrame().setVisible(true);
        });
    }
}
