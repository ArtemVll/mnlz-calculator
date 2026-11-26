package ru.spmi.mnlz.calc;

import static java.lang.Math.*;

/** Тепловой расчёт зоны вторичного охлаждения МНЛЗ — раздел 10 пособия Токового О.К. */
public final class ZvoCalculator {

    /** Теплоёмкость твёрдой стали Ст, кДж/(кг·К); теплота кристаллизации ΔL, кДж/кг. */
    private static final double CT = 0.678;
    private static final double DL = 270.0;
    /** Шаг роликов по участкам, м — средние значения по рис. 10.4. */
    private static final double[] ROLLER_PITCH = {0.29, 0.41, 0.52};
    /** Доли участков ЗВО: 0,2 / 0,6 / 0,2 (10.18). */
    private static final double[] SECTION_SHARE = {0.2, 0.6, 0.2};

    private ZvoCalculator() { }

    public static ZvoResult calculate(ZvoInput in) {
        validate(in);
        double tKr = in.steel().crystallizationC();       // Ткр, °C
        double tIn = in.surfaceInC();

        // (8.3) производительность МНЛЗ, кг/с: Gз = B·b·ρ·v/60
        double gRate = MnlzCalculator.DENSITY * in.widthM() * in.thicknessM() * in.castingSpeedMpm() / 60.0;

        // (10.4) теплосодержание заготовки при входе в ЗВО
        double halfT = CT * (tIn + tKr) / 2.0;
        double iIn = halfT + (in.shellThicknessM() / (in.thicknessM() / 2.0)) * (DL + halfT);

        // (10.5) теплосодержание полностью затвердевшей заготовки
        double iOut = CT * (in.surfaceOutC() + tKr) / 2.0;

        // (10.6) средняя температура поверхности в ЗВО
        double tAvg = (tIn + in.surfaceOutC()) / 2.0;

        // (10.8)-(10.9) периметр и длина ЗВО
        double perimeter = 2.0 * (in.widthM() + in.thicknessM());
        double zvoLength = in.liquidPoolM() - in.crystallizerLengthM();
        double surface = perimeter * zvoLength;                                   // (10.7)

        // (10.3) средний коэффициент теплоотдачи
        double alpha = gRate * (iIn - iOut) * 1e3 / (surface * (tAvg - in.waterTempC()));

        // (10.11)-(10.12) удельный и общий расход воды
        double gWater = (alpha - in.heatTransferBase()) / in.waterFactor();
        if (gWater <= 0) throw new IllegalStateException(
                "αΣ не превышает начальное значение α0 — проверьте параметры охлаждения");
        double waterFlow = gWater * surface;

        // (10.13)-(10.14) распределение по большому и малому радиусу
        double wMinor = 0.8 * waterFlow / 1.8;
        double wMajor = waterFlow / 1.8;

        // (10.18)-(10.19) три участка ЗВО
        ZvoSection[] sections = new ZvoSection[SECTION_SHARE.length];
        int totalGaps = 0;
        for (int i = 0; i < SECTION_SHARE.length; i++) {
            double len = SECTION_SHARE[i] * zvoLength;
            int gaps = max(1, (int) ceil(len / ROLLER_PITCH[i]));
            sections[i] = new ZvoSection(len, ROLLER_PITCH[i], gaps);
            totalGaps += gaps;
        }

        // (10.16) общее число форсунок и (10.17) расстояние до заготовки
        int nozzles = in.nozzlesPerRow() * totalGaps;
        double nozzleDist = (in.widthM() - 2.0 * in.shellThicknessM())
                / (2.0 * in.nozzlesPerRow() * tan(toRadians(in.nozzleAngleDeg() / 2.0)));

        return new ZvoResult(gRate, iIn, iOut, tAvg, perimeter, zvoLength, surface,
                alpha, gWater, waterFlow, wMinor, wMajor, sections, nozzles, nozzleDist);
    }

    private static void validate(ZvoInput in) {
        if (in.surfaceOutC() < 800) throw new IllegalArgumentException(
                "Температура поверхности на выходе из ЗВО не должна быть ниже 800 °C (разд. 10.1)");
        if (in.surfaceInC() <= in.surfaceOutC()) throw new IllegalArgumentException(
                "Температура на входе в ЗВО должна быть выше температуры на выходе");
        if (in.liquidPoolM() <= in.crystallizerLengthM()) throw new IllegalArgumentException(
                "Глубина лунки должна превышать длину кристаллизатора");
    }
}
