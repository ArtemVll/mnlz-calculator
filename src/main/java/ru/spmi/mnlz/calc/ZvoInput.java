package ru.spmi.mnlz.calc;

/** Исходные данные теплового расчёта ЗВО (разд. 10). */
public record ZvoInput(double widthM, double thicknessM,   // сечение заготовки, м
                       double castingSpeedMpm,             // v, м/мин
                       double liquidPoolM,                 // Lл из раздела 8
                       double crystallizerLengthM,         // lкр (обычно 0,7-1,1 м)
                       SteelGrade steel,
                       double surfaceInC,                  // Тп' на входе в ЗВО (из 9.20)
                       double shellThicknessM,             // δт (из 9.28)
                       double surfaceOutC,                 // Тп" на выходе (не ниже 800 °C)
                       double waterTempC,                  // Тв (обычно 20 °C)
                       double heatTransferBase,            // α0, 120-160 Вт/(м2*К)
                       double waterFactor,                 // B, 34-40 для радиальных/криволинейных
                       int nozzlesPerRow,                  // ni (в примере 4: по 2 плоскофакельных на сторону)
                       double nozzleAngleDeg,              // βэф (100° для плоскофакельных)
                       double shellKzMmMin05) { }          // kз для δт, 25-33 мм/мин^0,5
