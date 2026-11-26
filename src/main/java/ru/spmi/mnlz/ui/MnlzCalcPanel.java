package ru.spmi.mnlz.ui;

import ru.spmi.mnlz.auth.AuthService;
import ru.spmi.mnlz.calc.BilletType;
import ru.spmi.mnlz.calc.MnlzCalculator;
import ru.spmi.mnlz.calc.MnlzInput;
import ru.spmi.mnlz.calc.MnlzParameters;
import ru.spmi.mnlz.db.CalculationDao;
import ru.spmi.mnlz.calc.SteelGrade;
import ru.spmi.mnlz.db.SteelGradeDao;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Вкладка «Расчёт МНЛЗ»: исходные данные, расчёт по разделу 8, сохранение в базу. */
public final class MnlzCalcPanel extends JPanel {

    private final AuthService.Session session;
    private final SteelGradeDao gradeDao = new SteelGradeDao();
    private final CalculationDao calcDao = new CalculationDao();

    private final Map<String, SteelGrade> gradesByName = new LinkedHashMap<>();
    private final JComboBox<String> gradeBox = new JComboBox<>();
    private final JTextField chargeField    = new JTextField("120000", 10);
    private final JTextField widthField     = new JTextField("1.3", 10);
    private final JTextField thicknessField = new JTextField("0.16", 10);
    private final JTextField speedField     = new JTextField("1.2", 10);
    private final JTextField cycleField     = new JTextField("45", 10);
    private final JComboBox<String> factorBox = new JComboBox<>(new String[]{"0,5", "1,0", "1,5", "2,0"});
    private final JComboBox<BilletType> typeBox = new JComboBox<>(BilletType.values());

    private final JTextArea outputArea = new JTextArea();
    private final JButton saveButton = new JButton("Сохранить в базу");

    private MnlzInput lastInput;
    private MnlzParameters lastResult;
    private SteelGrade lastGrade;

    public MnlzCalcPanel(AuthService.Session session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- Левая часть: форма исходных данных
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Исходные данные"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        addRow(form, gc, 0, "Марка стали:", gradeBox);
        addRow(form, gc, 1, "Садка агрегата M, кг:", chargeField);
        addRow(form, gc, 2, "Ширина заготовки B, м:", widthField);
        addRow(form, gc, 3, "Толщина заготовки b, м:", thicknessField);
        addRow(form, gc, 4, "Скорость разливки v, м/мин:", speedField);
        addRow(form, gc, 5, "Цикл плавки, мин:", cycleField);
        addRow(form, gc, 6, "Кратность:", factorBox);
        addRow(form, gc, 7, "Тип заготовки:", typeBox);

        JButton calcButton = new JButton("Рассчитать");
        GridBagConstraints bc = (GridBagConstraints) gc.clone();
        bc.gridy = 8; bc.gridx = 1;
        form.add(calcButton, bc);

        saveButton.setEnabled(false);
        bc.gridy = 9;
        form.add(saveButton, bc);

        // ---- Правая часть: результаты
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Результаты расчёта"));

        add(form, BorderLayout.WEST);
        add(scroll, BorderLayout.CENTER);

        loadGrades();
        calcButton.addActionListener(e -> doCalculate());
        saveButton.addActionListener(e -> doSave());
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row,
                        String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    /** Загрузка справочника марок в выпадающий список. */
    private void loadGrades() {
        try {
            for (SteelGrade g : gradeDao.findAll()) {
                gradesByName.put(g.name(), g);
                gradeBox.addItem(g.name());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Не удалось загрузить марки: " + e.getMessage());
        }
    }

    /** Запуск расчёта ядра по данным формы. */
    private void doCalculate() {
        try {
            double charge   = parse(chargeField, "садка M");
            double width    = parse(widthField, "ширина B");
            double thickness = parse(thicknessField, "толщина b");
            double speed    = parse(speedField, "скорость v");
            double cycle    = parse(cycleField, "цикл плавки");
            double factor   = Double.parseDouble(
                    ((String) factorBox.getSelectedItem()).replace(',', '.'));
            BilletType type = (BilletType) typeBox.getSelectedItem();
            SteelGrade grade = gradesByName.get((String) gradeBox.getSelectedItem());
            if (grade == null) {
                JOptionPane.showMessageDialog(this, "Выберите марку стали");
                return;
            }
            lastInput = new MnlzInput(charge, width, thickness, speed, cycle, factor, type);
            lastResult = callCore(lastInput);
            lastGrade = grade;
            outputArea.setText(formatResult(grade, lastResult));
            saveButton.setEnabled(true);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Проверьте числовые поля: все значения должны быть числами");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка расчёта: " + ex.getMessage());
        }
    }

    /**
     * Единственная точка вызова ядра. Если у MnlzCalculator другое имя метода —
     * поправьте здесь, остальной код не меняется.
     */
    private MnlzParameters callCore(MnlzInput in) {
        return MnlzCalculator.calculate(in);
    }

    private String formatResult(SteelGrade grade, MnlzParameters r) {
        return "Марка: " + grade.name()
                + "  (углерод " + fmt(grade.carbonPercent()) + " %)" + System.lineSeparator()
                + System.lineSeparator()
                + "Число ручьёв n:                 " + r.streams() + System.lineSeparator()
                + "Цикл разливки, мин:             " + fmt(r.castingCycleMin()) + System.lineSeparator()
                + "Массовая скорость Gр, т/мин:    " + fmt(r.massRateTpm()) + System.lineSeparator()
                + "Расход заготовки Gз, кг/с:    " + fmt(r.billetRateKgs()) + System.lineSeparator()
                + "Глубина лунки Lл, м:            " + fmt(r.liquidPoolM()) + System.lineSeparator()
                + "Глубина лунки (проверка), м:    " + fmt(r.liquidPoolCheckM()) + System.lineSeparator()
                + "Радиус машины R = H, м:         " + fmt(r.radiusM()) + System.lineSeparator()
                + "Минимальный радиус Rmin, м:     " + fmt(r.minRadiusM()) + System.lineSeparator()
                + "Рекомендация типа МНЛЗ:         " + r.machineType() + System.lineSeparator();
    }

    /** Сохранение расчёта в базу через CalculationDao. */
    private void doSave() {
        if (lastResult == null) return;
        try {
            Map<String, Double> inputs = new LinkedHashMap<>();
            inputs.put("Садка M, кг", lastInput.chargeKg());
            inputs.put("Ширина B, м", lastInput.widthM());
            inputs.put("Толщина b, м", lastInput.thicknessM());
            inputs.put("Скорость v, м/мин", lastInput.castingSpeedMpm());
            inputs.put("Цикл плавки, мин", lastInput.meltingCycleMin());
            inputs.put("Кратность", lastInput.cycleFactor());
            inputs.put("Углерод марки, %", lastGrade.carbonPercent());

            Map<String, Double> results = new LinkedHashMap<>();
            results.put("Число ручьёв n", (double) lastResult.streams());
            results.put("Цикл разливки, мин", lastResult.castingCycleMin());
            results.put("Gр, т/мин", lastResult.massRateTpm());
            results.put("Gз, кг/мин", lastResult.billetRateKgs());
            results.put("Lл, м", lastResult.liquidPoolM());
            results.put("Lл проверка, м", lastResult.liquidPoolCheckM());
            results.put("R, м", lastResult.radiusM());
            results.put("Rmin, м", lastResult.minRadiusM());

            int id = calcDao.save(session.userId(), lastGrade.name(),
                    lastInput.widthM(), lastInput.thicknessM(), lastInput.castingSpeedMpm(),
                    inputs, results);
            JOptionPane.showMessageDialog(this, "Расчёт сохранён, запись № " + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
        }
    }

    private double parse(JTextField f, String what) {
        String s = f.getText().trim().replace(',', '.');
        if (s.isEmpty()) throw new NumberFormatException("Пустое поле: " + what);
        return Double.parseDouble(s);
    }

    private String fmt(double v) {
        return String.format("%.3f", v);
    }
}
