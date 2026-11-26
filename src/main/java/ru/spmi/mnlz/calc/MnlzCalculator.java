package ru.spmi.mnlz.calc;

import static java.lang.Math.*;

/** Расчёт основных параметров МНЛЗ — раздел 8 пособия Токового О.К. (ЮУрГУ, 2013). */
public final class MnlzCalculator {

    /** Плотность литой стали, кг/м3 (разд. 8.1.1). */
    public static final double DENSITY = 7300.0;

    /** Табл. 8.3: максимально допустимая продолжительность разливки, мин. */
    private static final double[] LADLE_CAPACITY = {12, 25, 50, 100, 160, 200, 300};
    private static final double[] MAX_CASTING_TIME = {40, 50, 60, 75, 85, 90, 110};

    private MnlzCalculator() { }

    public static MnlzParameters calculate(MnlzInput in) {
        validate(in);
        // Цикл разливки кратен циклу плавки: tau_р = k·tau_пл
        double castingCycle = in.cycleFactor() * in.meltingCycleMin();
        if (castingCycle > maxCastingTime(in.chargeKg()) + 1e-9) {
            throw new IllegalArgumentException(String.format(
                    "Цикл разливки %.1f мин превышает допустимый %.1f мин для ковша %.0f т (табл. 8.3)",
                    castingCycle, maxCastingTime(in.chargeKg()), in.chargeKg() / 1000.0));
        }

        // (8.2) число ручьёв; в пособии 1,05 ручья принимают за 1 — округление до целого
        double nRaw = in.chargeKg() / (in.widthM() * in.thicknessM() * DENSITY
                * in.castingSpeedMpm() * castingCycle);
        int n = max(1, (int) round(nRaw));

        // (8.1) массовая скорость разливки, т/мин
        double massRate = in.chargeKg() / (0.9 * n * castingCycle) / 1000.0;

        // (8.3) массовая скорость выхода заготовки, кг/мин
        double billetRate = in.widthM() * in.thicknessM() * DENSITY * in.castingSpeedMpm();

        // (8.5) глубина лунки жидкой фазы и проверка по (8.4)
        double pool = in.type().kSolidification * in.thicknessM() * in.chargeKg()
                / (0.9 * n * in.widthM() * castingCycle * DENSITY);
        double poolCheck = in.type().kSolidification * in.thicknessM() * in.thicknessM()
                * in.castingSpeedMpm();

        // (8.7) радиус и высота МНЛЗ радиального типа
        double radius = 2.0 * pool / PI;
        // (8.8)-(8.9) минимально допустимый радиус
        double minRadius = in.type().rMinFactor * in.thicknessM();

        // Рекомендация типа МНЛЗ по предельной протяжённости жидкой фазы (разд. 8.1.1)
        String machineType;
        if (pool <= 16)      machineType = "Вертикальная (предельная жидкая фаза 16 м)";
        else if (pool <= 19) machineType = "Радиальная с выпрямлением полностью затвердевшей заготовки (19 м)";
        else if (pool <= 30) machineType = "Радиальная с выпрямлением в двухфазном состоянии (30 м)";
        else                 machineType = "Криволинейная с дифференцированным выпрямлением (35 м)";

        return new MnlzParameters(n, castingCycle, massRate, billetRate,
                pool, poolCheck, radius, minRadius, machineType);
    }

    /** Линейная интерполяция табл. 8.3 по ёмкости ковша. */
    public static double maxCastingTime(double chargeKg) {
        double t = chargeKg / 1000.0;
        if (t <= LADLE_CAPACITY[0]) return MAX_CASTING_TIME[0];
        if (t >= LADLE_CAPACITY[LADLE_CAPACITY.length - 1]) return MAX_CASTING_TIME[LADLE_CAPACITY.length - 1];
        for (int i = 1; i < LADLE_CAPACITY.length; i++) {
            if (t <= LADLE_CAPACITY[i]) {
                double w = (t - LADLE_CAPACITY[i - 1]) / (LADLE_CAPACITY[i] - LADLE_CAPACITY[i - 1]);
                return MAX_CASTING_TIME[i - 1] + w * (MAX_CASTING_TIME[i] - MAX_CASTING_TIME[i - 1]);
            }
        }
        return MAX_CASTING_TIME[MAX_CASTING_TIME.length - 1];
    }

    private static void validate(MnlzInput in) {
        if (in.chargeKg() <= 0 || in.widthM() <= 0 || in.thicknessM() <= 0 || in.castingSpeedMpm() <= 0)
            throw new IllegalArgumentException("Все исходные параметры должны быть положительными");
        if (in.cycleFactor() <= 0 || in.meltingCycleMin() <= 0)
            throw new IllegalArgumentException("Цикл плавки и кратность должны быть положительными");
    }
}
