package ru.spmi.mnlz.ui;

import ru.spmi.mnlz.auth.AuthService;
import ru.spmi.mnlz.calc.SteelGrade;
import ru.spmi.mnlz.calc.ZvoCalculator;
import ru.spmi.mnlz.calc.ZvoInput;
import ru.spmi.mnlz.calc.ZvoResult;
import ru.spmi.mnlz.calc.ZvoSection;
import ru.spmi.mnlz.db.CalculationDao;
import ru.spmi.mnlz.db.SteelGradeDao;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Вкладка «Расчёт ЗВО»: тепровой расчёт зоны вторичного охлаждения (разд. 10). */
public final class ZvoCalcPanel extends JPanel {

    private final AuthService.Session session;
    private final SteelGradeDao gradeDao = new SteelGradeDao();
    private final CalculationDao calcDao = new CalculationDao();

    private final Map<String, SteelGrade> gradesByName = new LinkedHashMap<>();
    private final JComboBox<String> gradeBox = new JComboBox<>();
    private final JTextField widthField      = new JTextField("1.3", 8);
    private final JTextField thicknessField  = new JTextField("0.16", 8);
    private final JTextField speedField      = new JTextField("1.3", 8);
    private final JTextField poolField       = new JTextField("13.2", 8);
    private final JTextField crystallizerField = new JTextField("1.0", 8);
    private final JTextField surfaceInField  = new JTextField("1091", 8);
    private final JTextField shellField      = new JTextField("0.0249", 8);
    private final JTextField surfaceOutField = new JTextField("900", 8);
    private final JTextField waterTempField  = new JTextField("20", 8);
    private final JTextField alphaBaseField  = new JTextField("140", 8);
    private final JTextField waterFactorField = new JTextField("37", 8);
    private final JTextField nozzlesField    = new JTextField("4", 8);
    private final JTextField angleField      = new JTextField("100", 8);
    private final JTextField kzField         = new JTextField("28", 8);

    private final JTextArea outputArea = new JTextArea();
    private final JButton saveButton = new JButton("Сохранить в базу");

    private ZvoInput lastInput;
    private ZvoResult lastResult;
    private SteelGrade lastGrade;

    public ZvoCalcPanel(AuthService.Session session) {
        this.session = session;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Исходные данные (разд. 10)"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 3, 3, 3);
        gc.anchor = GridBagConstraints.WEST;

        addRow(form, gc, 0,  "Марка стали:", gradeBox);
        addRow(form, gc, 1,  "Ширина заготовки B, м:", widthField);
        addRow(form, gc, 2,  "Толщина заготовки b, м:", thicknessField);
        addRow(form, gc, 3,  "Скорость разливки v, м/мин:", speedField);
        addRow(form, gc, 4,  "Глубина лунки Lл, м (из разд. 8):", poolField);
        addRow(form, gc, 5,  "Длина кристаллизатора lкр, м:", crystallizerField);
        addRow(form, gc, 6,  "Тп' на входе в ЗВО, °C:", surfaceInField);
        addRow(form, gc, 7,  "Толщина корки δт, м (из 9.28):", shellField);
        addRow(form, gc, 8,  "Тп\" на выходе, °C (≥ 800):", surfaceOutField);
        addRow(form, gc, 9,  "Температура воды Тв, °C:", waterTempField);
        addRow(form, gc, 10, "α0, Вт/(м²·К) (120–160):", alphaBaseField);
        addRow(form, gc, 11, "Коэффициент B (34–40):", waterFactorField);
        addRow(form, gc, 12, "Форсунок в ряду ni:", nozzlesField);
        addRow(form, gc, 13, "Угол форсунки βэф, °:", angleField);
        addRow(form, gc, 14, "kз, мм/мин^0,5 (25–33):", kzField);

        JButton calcButton = new JButton("Рассчитать");
        GridBagConstraints bc = (GridBagConstraints) gc.clone();
        bc.gridy = 15; bc.gridx = 1;
        form.add(calcButton, bc);

        saveButton.setEnabled(false);
        bc.gridy = 16;
        form.add(saveButton, bc);

        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Результаты расчёта ЗВО"));

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

    private void doCalculate() {
        try {
            SteelGrade grade = gradesByName.get((String) gradeBox.getSelectedItem());
            if (grade == null) {
                JOptionPane.showMessageDialog(this, "Выберите марку стали");
                return;
            }
            lastInput = new ZvoInput(
                    parse(widthField, "ширина B"),
                    parse(thicknessField, "толщина b"),
                    parse(speedField, "скорость v"),
                    parse(poolField, "глубина лунки Lл"),
                    parse(crystallizerField, "длина кристаллизатора"),
                    grade,
                    parse(surfaceInField, "Тп' на входе"),
                    parse(shellField, "толщина корки δт"),
                    parse(surfaceOutField, "Тп\" на выходе"),
                    parse(waterTempField, "температура воды"),
                    parse(alphaBaseField, "α0"),
                    parse(waterFactorField, "коэффициент B"),
                    (int) Double.parseDouble(nozzlesField.getText().trim().replace(',', '.')),
                    parse(angleField, "угол форсунки"),
                    parse(kzField, "kз"));
            lastResult = ZvoCalculator.calculate(lastInput);
            lastGrade = grade;
            outputArea.setText(formatResult(grade, lastResult));
            saveButton.setEnabled(true);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Проверьте числовые поля: все значения должны быть числами");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Валидатор ядра вернул понятное сообщение — показываем как есть
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (Exception ex) {

            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка расчёта: " + ex.getMessage());
        }
    }

    private String formatResult(SteelGrade grade, ZvoResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Марка: ").append(grade.name())
                .append("  (Ткр ").append(fmt(grade.crystallizationC())).append(" °C)")
                .append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("Gз, кг/с:                    ").append(fmt(r.billetRateKgs())).append(System.lineSeparator());
        sb.append("iн, кДж/кг:                  ").append(fmt(r.heatContentIn())).append(System.lineSeparator());
        sb.append("iк, кДж/кг:                  ").append(fmt(r.heatContentOut())).append(System.lineSeparator());
        sb.append("Тп ср, °C:                   ").append(fmt(r.avgSurfaceTempC())).append(System.lineSeparator());
        sb.append("Периметр П, м:               ").append(fmt(r.perimeterM())).append(System.lineSeparator());
        sb.append("Длина ЗВО lЗВО, м:           ").append(fmt(r.zvoLengthM())).append(System.lineSeparator());
        sb.append("Поверхность F, м²:           ").append(fmt(r.surfaceM2())).append(System.lineSeparator());
        sb.append("αΣ, Вт/(м²·К):               ").append(fmt(r.heatTransferCoef())).append(System.lineSeparator());
        sb.append("Удельный расход g, м³/(м²·ч):").append(fmt(r.specificWater())).append(System.lineSeparator());
        sb.append("Общий расход Gв, м³/ч:       ").append(fmt(r.waterFlow())).append(System.lineSeparator());
        sb.append("По малому радиусу, м³/ч:     ").append(fmt(r.waterFlowMinorR())).append(System.lineSeparator());
        sb.append("По большому радиусу, м³/ч:   ").append(fmt(r.waterFlowMajorR())).append(System.lineSeparator());
        sb.append("Всего форсунок nф:           ").append(r.totalNozzles()).append(System.lineSeparator());
        sb.append("Расстояние до заготовки h, м:").append(fmt(r.nozzleDistanceM())).append(System.lineSeparator());
        sb.append(System.lineSeparator()).append("Участки ЗВО (10.18)-(10.19):").append(System.lineSeparator());
        for (int i = 0; i < r.sections().length; i++) {
            ZvoSection s = r.sections()[i];
            sb.append(String.format("  Участок %d: длина %.3f м, шаг роликов %.2f м, промежутков %d%n",
                    i + 1, s.lengthM(), s.rollerPitchM(), s.gaps()));
        }
        return sb.toString();
    }

    private void doSave() {
        if (lastResult == null) return;
        try {
            Map<String, Double> inputs = new LinkedHashMap<>();
            inputs.put("Ширина B, м", lastInput.widthM());
            inputs.put("Толщина b, м", lastInput.thicknessM());
            inputs.put("Скорость v, м/мин", lastInput.castingSpeedMpm());
            inputs.put("Глубина лунки Lл, м", lastInput.liquidPoolM());
            inputs.put("Длина кристаллизатора, м", lastInput.crystallizerLengthM());
            inputs.put("Тп' вход, °C", lastInput.surfaceInC());
            inputs.put("Толщина корки δт, м", lastInput.shellThicknessM());
            inputs.put("Тп\" выход, °C", lastInput.surfaceOutC());
            inputs.put("Тв, °C", lastInput.waterTempC());
            inputs.put("α0, Вт/(м²·К)", lastInput.heatTransferBase());
            inputs.put("Коэффициент B", lastInput.waterFactor());
            inputs.put("Форсунок в ряду ni", (double) lastInput.nozzlesPerRow());
            inputs.put("Угол βэф, °", lastInput.nozzleAngleDeg());
            inputs.put("kз, мм/мин^0,5", lastInput.shellKzMmMin05());

            Map<String, Double> results = new LinkedHashMap<>();
            results.put("Gз, кг/с", lastResult.billetRateKgs());
            results.put("iн, кДж/кг", lastResult.heatContentIn());
            results.put("iк, кДж/кг", lastResult.heatContentOut());
            results.put("Тп ср, °C", lastResult.avgSurfaceTempC());
            results.put("Периметр П, м", lastResult.perimeterM());
            results.put("Длина ЗВО, м", lastResult.zvoLengthM());
            results.put("Поверхность F, м²", lastResult.surfaceM2());
            results.put("αΣ, Вт/(м²·К)", lastResult.heatTransferCoef());
            results.put("g, м³/(м²·ч)", lastResult.specificWater());
            results.put("Gв, м³/ч", lastResult.waterFlow());
            results.put("Малый радиус, м³/ч", lastResult.waterFlowMinorR());
            results.put("Большой радиус, м³/ч", lastResult.waterFlowMajorR());
            results.put("Форсунок всего", (double) lastResult.totalNozzles());
            results.put("Расстояние h, м", lastResult.nozzleDistanceM());
            ZvoSection[] sec = lastResult.sections();
            for (int i = 0; i < sec.length; i++) {
                results.put("Участок " + (i + 1) + " длина, м", sec[i].lengthM());
                results.put("Участок " + (i + 1) + " промежутков", (double) sec[i].gaps());
            }

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
