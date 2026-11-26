package ru.spmi.mnlz.ui;

import ru.spmi.mnlz.auth.AuthService;

import javax.swing.*;
import java.awt.*;

/** Главное окно приложения: вкладки расчётов, базы и справки. */
public final class MainFrame extends JFrame {

    private final AuthService.Session session;

    public MainFrame(AuthService.Session session) {
        super("Калькулятор металлурга — " + session.username() + " [" + session.role() + "]");
        this.session = session;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Расчёт МНЛЗ", new MnlzCalcPanel(session));
        tabs.addTab("Расчёт ЗВО", new ZvoCalcPanel(session));
        tabs.addTab("База данных", new DatabasePanel(session));
        tabs.addTab("Справка", new HelpPanel());
        add(tabs);

        setSize(900, 620);
        setLocationRelativeTo(null);
    }

    private JPanel placeholder(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(text);
        label.setForeground(Color.GRAY);
        panel.add(label);
        return panel;
    }
}
