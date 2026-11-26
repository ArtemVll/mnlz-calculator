package ru.spmi.mnlz.calc;

/** Результаты расчёта раздела 8. */
public record MnlzParameters(int streams,          // число ручьёв n
                             double castingCycleMin,  // цикл разливки tau_р
                             double massRateTpm,   // Gр, т/мин (8.1)
                             double billetRateKgm, // Gз, кг/мин (8.3)
                             double liquidPoolM,   // глубина лунки Lл (8.5)
                             double liquidPoolCheckM, // проверка Lл по (8.4)
                             double radiusM,       // R = H (8.7)
                             double minRadiusM,    // Rmin (8.8)-(8.9)
                             String machineType) { }   // рекомендация типа МНЛЗ
