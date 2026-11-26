package ru.spmi.mnlz.calc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Проверка на контрольном примере пособия: сляб 1,3x0,16 м, Ст3сп, конвертер 140 т, v = 1,3 м/мин. */
class CalculatorTest {

    @Test
    void mnlzParametersMatchTextbookExample() {
        MnlzParameters r = MnlzCalculator.calculate(
                new MnlzInput(140_000, 1.3, 0.16, 1.3, 45.0, 1.5, BilletType.SLAB_WIDE));
        assertEquals(1, r.streams());                    // пособие: n = 1
        assertEquals(13.2, r.liquidPoolM(), 0.15);       // пособие: Lл = 13,2 м
        assertEquals(8.4, r.radiusM(), 0.15);            // пособие: R = H = 8,4 м
        assertEquals(6.72, r.minRadiusM(), 0.01);        // Rmin = 42b = 6,72 м
    }

    @Test
    void zvoHeatBalanceMatchesTextbookFormulas() {
        SteelGrade st3sp = SteelGrade.ST3SP;
        double surfaceIn = CrystallizerAux.surfaceTempC(st3sp, 1.3, 0.9, 0.025);
        assertEquals(1091, surfaceIn, 3.0);              // пособие: Тп = 1091 °C
        double shell = CrystallizerAux.shellThicknessM(1.3, 0.9, 30.0);
        assertEquals(0.0249, shell, 0.002);              // пособие: δт = 24,9 мм

        MnlzParameters mp = MnlzCalculator.calculate(
                new MnlzInput(140_000, 1.3, 0.16, 1.3, 45.0, 1.5, BilletType.SLAB_WIDE));
        ZvoResult z = ZvoCalculator.calculate(new ZvoInput(
                1.3, 0.16, 1.3, mp.liquidPoolM(), 1.0, st3sp,
                surfaceIn, shell, 900.0, 20.0, 140.0, 37.0, 4, 100.0, 30.0));
        assertEquals(32.9, z.billetRateKgs(), 0.1);      // Gз = 32,9 кг/с
        assertEquals(822.8, z.heatContentOut(), 0.5);    // iк = 822,8 кДж/кг
        assertEquals(35.62, z.surfaceM2(), 0.3);         // F = 35,62 м2
        // iн по формуле (10.4) = 1247,8; в примере пособия напечатано 1361,8 — см. примечание
        assertEquals(1247.8, z.heatContentIn(), 1.0);
    }
}
