package ru.spmi.mnlz;

import javax.swing.*;

/** Точка входа «Калькулятора металлурга». */
public class Main {

    public static void main(String[] args) {
        // Все операции с интерфейсом — в потоке событий Swing
        SwingUtilities.invokeLater(() -> {
            // TODO: здесь будет окно логина (этап 4)
            JFrame frame = new JFrame("Калькулятор металлурга");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(500, 300);
            frame.setLocationRelativeTo(null); // по центру экрана
            frame.setVisible(true);
        });
    }
}
