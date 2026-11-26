package ru.spmi.mnlz.ui;

import ru.spmi.mnlz.auth.AuthService;

import javax.swing.*;
import java.awt.*;

/** Окно входа: логин и пароль, проверка через AuthService. */
public final class LoginFrame extends JFrame {

    private final JTextField loginField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JLabel messageLabel = new JLabel(" ");

    public LoginFrame() {
        super("Вход — Калькулятор металлурга");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0;
        add(new JLabel("Логин:"), gc);
        gc.gridx = 1;
        add(loginField, gc);

        gc.gridx = 0; gc.gridy = 1;
        add(new JLabel("Пароль:"), gc);
        gc.gridx = 1;
        add(passwordField, gc);

        JButton loginButton = new JButton("Войти");
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2;
        add(loginButton, gc);

        messageLabel.setForeground(Color.RED);
        gc.gridy = 3;
        add(messageLabel, gc);

        // Вход и по Enter в поле пароля, и по кнопке
        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());

        setSize(360, 220);
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String username = loginField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isEmpty()) {
            messageLabel.setText("Введите логин и пароль");
            return;
        }
        try {
            AuthService.Session session = new AuthService().login(username, password);
            if (session == null) {
                messageLabel.setText("Неверный логин или пароль");
                return;
            }
            // Успех: открываем главное окно, логин закрываем
            JOptionPane.showMessageDialog(this,
                    "Вход выполнен: " + session.username() + " (" + session.role() + ")");
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            messageLabel.setText("Ошибка: " + ex.getMessage());
        }
    }
}
