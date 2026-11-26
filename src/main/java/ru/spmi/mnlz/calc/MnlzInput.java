package ru.spmi.mnlz.calc;

/** Исходные данные для расчёта основных параметров МНЛЗ (разд. 8). */
public record MnlzInput(double chargeKg,          // садка агрегата М, кг
                        double widthM,            // ширина заготовки B, м
                        double thicknessM,        // толщина заготовки b, м
                        double castingSpeedMpm,   // скорость разливки v, м/мин
                        double meltingCycleMin,   // цикл плавки, мин
                        double cycleFactor,       // кратность: 0,5; 1,0; 2,0 ...
                        BilletType type) { }
