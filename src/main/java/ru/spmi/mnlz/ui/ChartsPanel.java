package ru.spmi.mnlz.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import ru.spmi.mnlz.calc.BilletType;
import ru.spmi.mnlz.calc.MnlzCalculator;
import ru.spmi.mnlz.calc.MnlzInput;
import ru.spmi.mnlz.calc.MnlzParameters;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Вкладка «Графики»: зависимости параметров МНЛЗ от скорости разливки. */
public final class ChartsPanel extends JPanel {

    private final JTextField chargeField    = new JTextField("120000", 8);
    private final JTextField widthField     = new JTextField("1.3", 8);
    private final JTextField thicknessField = new JTextField("0.16", 8);
    private final JTextField cycleField     = new JTextField("45", 8);
    private final JComboBox<String> factorBox = new JComboBox<>(new String[]{"0,5", "1,0", "2,0"});
    private final JComboBox<BilletType> typeBox = new JComboBox<>(BilletType.values());

    private static final String[] CHART_TYPES = {
            "Глубина лунки Lл от скорости разливки",
            "Массовая скорость Gр от скорости разливки",
            "Минимальный радиус Rmin от скорости разливки"
    };
    private final JComboBox<String> chartTypeBox = new JComboBox<>(CHART_TYPES);

    private ChartPanel chartPanel;
    private JFreeChart currentChart;

    public ChartsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Исходные данные"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        addRow(form, gc, 0, "Садка агрегата M, кг:", chargeField);
        addRow(form, gc, 1, "Ширина заготовки B, м:", widthField);
        addRow(form, gc, 2, "Толщина заготовки b, м:", thicknessField);
        addRow(form, gc, 3, "Цикл плавки, мин:", cycleField);
        addRow(form, gc, 4, "Кратность:", factorBox);
        addRow(form, gc, 5, "Тип заготовки:", typeBox);
        addRow(form, gc, 6, "Вид графика:", chartTypeBox);

        JButton build = new JButton("Построить");
        GridBagConstraints bc = (GridBagConstraints) gc.clone();
        bc.gridy = 7; bc.gridx = 1;
        form.add(build, bc);

        JButton savePng = new JButton("Сохранить PNG");
        bc.gridy = 8;
        form.add(savePng, bc);

        add(form, BorderLayout.WEST);
        JLabel hint = new JLabel("Задайте данные и нажмите «Построить»");
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        add(hint, BorderLayout.CENTER);

        build.addActionListener(e -> doBuild());
        savePng.addActionListener(e -> doSavePng());
    }

    private void addRow(JPanel form, GridBagConstraints gc, int row,
                        String label, JComponent field) {
        gc.gridx = 0; gc.gridy = row;
        form.add(new JLabel(label), gc);
        gc.gridx = 1;
        form.add(field, gc);
    }

    /** Прогон ядра по диапазону скоростей 0,4–2,4 м/мин и построение кривой. */
    private void doBuild() {
        try {
            double charge = parse(chargeField, "садка M");
            double width = parse(widthField, "ширина B");
            double thickness = parse(thicknessField, "толщина b");
            double cycle = parse(cycleField, "цикл плавки");
            double factor = Double.parseDouble(
                    ((String) factorBox.getSelectedItem()).replace(',', '.'));
            BilletType type = (BilletType) typeBox.getSelectedItem();

            int kind = chartTypeBox.getSelectedIndex();
            XYSeries series = new XYSeries(yAxisName(kind));
            for (double v = 0.4; v <= 2.41; v = Math.round((v + 0.1) * 10) / 10.0) {
                try {
                    MnlzParameters p = MnlzCalculator.calculate(
                            new MnlzInput(charge, width, thickness, v, cycle, factor, type));
                    double y = switch (kind) {
                        case 0 -> p.liquidPoolM();
                        case 1 -> p.massRateTpm();
                        default -> p.minRadiusM();
                    };
                    series.add(v, y);
                } catch (Exception ignored) {
                    // точка вне допустимой области — просто пропускаем
                }
            }
            if (series.getItemCount() < 2) {
                JOptionPane.showMessageDialog(this,
                        "В выбранном диапазоне скорости нет допустимых точек");
                return;
            }
            currentChart = ChartFactory.createXYLineChart(
                    CHART_TYPES[kind], "Скорость разливки v, м/мин", yAxisName(kind),
                    new XYSeriesCollection(series),
                    PlotOrientation.VERTICAL, true, true, false);

            if (chartPanel == null) {
                chartPanel = new ChartPanel(currentChart);
                add(chartPanel, BorderLayout.CENTER);
            } else {
                chartPanel.setChart(currentChart);
            }
            revalidate();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Проверьте числовые поля");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ошибка построения: " + ex.getMessage());
        }
    }

    private String yAxisName(int kind) {
        return switch (kind) {
            case 0 -> "Глубина лунки Lл, м";
            case 1 -> "Массовая скорость Gр, т/мин";
            default -> "Минимальный радиус Rmin, м";
        };
    }

    /** Сохранение текущего графика в PNG — удобно для приложений к записке. */
    private void doSavePng() {
        if (currentChart == null) {
            JOptionPane.showMessageDialog(this, "Сначала постройте график");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("chart.png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }
        try {
            ChartUtils.saveChartAsPNG(file, currentChart, 1000, 700);
            JOptionPane.showMessageDialog(this, "Сохранено: " + file.getAbsolutePath());
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
}
