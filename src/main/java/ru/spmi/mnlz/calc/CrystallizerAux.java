package ru.spmi.mnlz.calc;

import static java.lang.Math.*;

/** Подготовка исходных данных для теплового расчёта ЗВО: температура поверхности
 *  заготовки и толщина корочки на выходе из кристаллизатора (разд. 9.2 пособия). */
public final class CrystallizerAux {

    private CrystallizerAux() { }

    /**
     * Температура поверхности заготовки на выходе из кристаллизатора — уравнение (9.20).
     * В контрольном примере пособия tau берётся по активной высоте la (0,9 м при lк = 1,0 м).
     */
    public static double surfaceTempC(SteelGrade steel, double speedMpm, double activeHeightM, double kzMpm05) {
        // ТП = ТС - A*v^m * kЗ * sqrt(la/v) / (2*lambda_M*1e-3),  q в кВт/м2
        double dT = steel.aHeat() * pow(speedMpm, steel.mHeat()) * kzMpm05
                * sqrt(activeHeightM / speedMpm) / (2.0 * steel.lambdaM() * 1e-3);
        return steel.solidusK() - dT - 273.15;
    }

    /** Толщина твёрдой корочки на выходе из кристаллизатора — уравнения (9.28)-(9.29), м. */
    public static double shellThicknessM(double speedMpm, double activeHeightM, double kzMmMin05) {
        double tau = activeHeightM / speedMpm;         // (9.29), мин
        return kzMmMin05 * sqrt(tau) / 1000.0;         // (9.28), м
    }
}
