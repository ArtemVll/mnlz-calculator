package ru.spmi.mnlz.ui;

import ru.spmi.mnlz.auth.AuthService;
import ru.spmi.mnlz.calc.SteelGrade;
import ru.spmi.mnlz.db.CalculationDao;
import ru.spmi.mnlz.db.EquipmentDao;
import ru.spmi.mnlz.db.HistoryEntry;
import ru.spmi.mnlz.db.Machine;
import ru.spmi.mnlz.db.SteelGradeDao;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Вкладка «База данных»: история расчётов, оборудование, справочник марок. */
public final class DatabasePanel extends JPanel {

    private final AuthService.Session session;
    private final CalculationDao calcDao = new CalculationDao();
    private final EquipmentDao equipDao = new EquipmentDao();
    private final SteelGradeDao gradeDao = new SteelGradeDao();

    // --- История расчётов
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[]{"№", "Марка", "Ширина, м", "Толщина, м", "Скорость, м/мин", "Дата"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable historyTable = new JTable(historyModel);
    private final Map<Integer, HistoryEntry> historyById = new HashMap<>();
    private final JTextArea detailArea = new JTextArea();
    private final JTextField filterWidth = new JTextField(5);
    private final JTextField filterThickness = new JTextField(5);
    private final JButton deleteButton = new JButton("Удалить запись");

    // --- Оборудование
    private final DefaultTableModel equipModel = new DefaultTableModel(
            new Object[]{"Машина", "Тип", "Радиус, м", "Ручьёв", "Ёмкость, т"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // --- Марки стали
    private final DefaultTableModel gradeModel = new DefaultTableModel(
            new Object[]{"Марка", "Углерод, %", "Ткр, °C"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTextField gradeSearch = new JTextField(12);
    private List<SteelGrade> allGrades = new ArrayList<>();

    public DatabasePanel(AuthService.Session session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTabbedPane inner = new JTabbedPane();
        inner.addTab("История расчётов", buildHistoryTab());
        inner.addTab("Оборудование МНЛЗ", buildEquipmentTab());
        inner.addTab("Марки стали", buildGradesTab());
        add(inner, BorderLayout.CENTER);

        // Удаление доступно только администратору — разграничение прав
        deleteButton.setEnabled("admin".equalsIgnoreCase(session.role()));
        if (!deleteButton.isEnabled()) {
            deleteButton.setToolTipText("Удаление доступно только администратору");
        }
        deleteButton.addActionListener(e -> doDelete());
        loadHistory(null, null);
        loadEquipment();
        loadGrades();
    }

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Фильтр по сечению: ширина"));
        top.add(filterWidth);
        top.add(new JLabel("толщина"));
        top.add(filterThickness);
        JButton apply = new JButton("Применить");
        JButton reset = new JButton("Сбросить");
        top.add(apply);
        top.add(reset);
        panel.add(top, BorderLayout.NORTH);

        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setAutoCreateRowSorter(true);
        historyTable.getSelectionModel().addListSelectionListener(e -> showDetails());
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        detailArea.setEditable(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setPreferredSize(new Dimension(0, 180));
        detailScroll.setBorder(BorderFactory.createTitledBorder("Параметры выбранного расчёта"));
        panel.add(detailScroll, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(deleteButton);
        panel.add(bottom, BorderLayout.SOUTH);

        apply.addActionListener(e -> {
            try {
                Double w = filterWidth.getText().isBlank() ? null
                        : Double.valueOf(filterWidth.getText().trim().replace(',', '.'));
                Double t = filterThickness.getText().isBlank() ? null
                        : Double.valueOf(filterThickness.getText().trim().replace(',', '.'));
                loadHistory(w, t);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Фильтр: введите числа");
            }
        });
        reset.addActionListener(e -> {
            filterWidth.setText("");
            filterThickness.setText("");
            loadHistory(null, null);
        });
        return panel;
    }

    /** Загрузка истории; если w и t заданы — фильтрация по сечению. */
    private void loadHistory(Double w, Double t) {
        try {
            historyModel.setRowCount(0);
            historyById.clear();
            detailArea.setText("");
            List<HistoryEntry> list = (w == null && t == null)
                    ? calcDao.findRecent(200) : calcDao.filterBySection(w, t);
            for (HistoryEntry h : list) {
                historyById.put(h.id(), h);
                historyModel.addRow(new Object[]{
                        h.id(), h.steel(), h.width(), h.thickness(), h.speed(), h.created()});
            }
            if (list.isEmpty()) detailArea.setText("Записей не найдено");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка чтения истории: " + ex.getMessage());
        }
    }

    /** Подробности выбранной записи: все входные и выходные параметры. */
    private void showDetails() {
        int viewRow = historyTable.getSelectedRow();
        if (viewRow < 0) return;
        int id = (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(viewRow), 0);
        HistoryEntry h = historyById.get(id);
        if (h == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Расчёт № ").append(h.id()).append(" | марка ").append(h.steel())
                .append(" | сечение ").append(h.width()).append(" × ").append(h.thickness())
                .append(" м | v = ").append(h.speed()).append(" м/мин | ").append(h.created())
                .append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("-- Входные параметры:").append(System.lineSeparator());
        h.inputs().forEach((k, v) -> sb.append(String.format("  %-28s %g%n", k, v)));
        sb.append("-- Результаты:").append(System.lineSeparator());
        h.results().forEach((k, v) -> sb.append(String.format("  %-28s %g%n", k, v)));
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    /** Удаление выбранного расчёта — только для роли admin. */
    private void doDelete() {
        int viewRow = historyTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Выберите строку в таблице истории");
            return;
        }
        int id = (int) historyModel.getValueAt(historyTable.convertRowIndexToModel(viewRow), 0);
        int answer = JOptionPane.showConfirmDialog(this,
                "Удалить расчёт № " + id + " вместе с его параметрами?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            calcDao.delete(id);
            loadHistory(null, null);
            filterWidth.setText("");
            filterThickness.setText("");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка удаления: " + ex.getMessage());
        }
    }

    private JPanel buildEquipmentTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(equipModel);
        table.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGradesTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Поиск по названию:"));
        top.add(gradeSearch);
        JButton find = new JButton("Найти");
        top.add(find);
        panel.add(top, BorderLayout.NORTH);

        JTable table = new JTable(gradeModel);
        table.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        find.addActionListener(e -> applyGradeFilter());
        gradeSearch.addActionListener(e -> applyGradeFilter());
        return panel;
    }

    private void loadEquipment() {
        try {
            for (Machine m : equipDao.findAll()) {
                equipModel.addRow(new Object[]{
                        m.name(), m.type(), m.radiusM(), m.streams(), m.ladleT()});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка чтения оборудования: " + ex.getMessage());
        }
    }

    private void loadGrades() {
        try {
            allGrades = gradeDao.findAll();
            fillGradeTable(allGrades);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка чтения марок: " + ex.getMessage());
        }
    }

    /** Операция «поиск»: фильтр справочника марок по подстроке названия. */
    private void applyGradeFilter() {
        String q = gradeSearch.getText().trim().toLowerCase();
        List<SteelGrade> shown = new ArrayList<>();
        for (SteelGrade g : allGrades) {
            if (q.isEmpty() || g.name().toLowerCase().contains(q)) shown.add(g);
        }
        fillGradeTable(shown);
    }

    private void fillGradeTable(List<SteelGrade> grades) {
        gradeModel.setRowCount(0);
        for (SteelGrade g : grades) {
            gradeModel.addRow(new Object[]{g.name(), g.carbonPercent(), g.crystallizationC()});
        }
    }
}
