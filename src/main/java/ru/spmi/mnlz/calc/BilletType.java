package ru.spmi.mnlz.calc;

/** Тип заготовки: определяет коэффициент полного затвердевания kз (разд. 8.1.1) и Rmin. */
public enum BilletType {
    SLAB_WIDE(340.0, 42.0),   // сляб с B > 1200 мм
    SLAB_NARROW(290.0, 42.0), // сляб с B < 1200 мм
    BILLET(240.0, 22.8);      // сортовая заготовка

    public final double kSolidification; // kз, мин/м
    public final double rMinFactor;      // Rmin >= 42b (слябы) / 22,8b (сортовая), ур. (8.8)-(8.9)

    BilletType(double kSolidification, double rMinFactor) {
        this.kSolidification = kSolidification;
        this.rMinFactor = rMinFactor;
    }
}
