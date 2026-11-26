package ru.spmi.mnlz.calc;

/** Марка стали: справочные данные для тепловых расчётов МНЛЗ (разд. 9 пособия Токового). */
public record SteelGrade(String name, double carbonPercent, double lambdaM, double aHeat, double mHeat) {

    /** λМ — теплопроводность стали, Вт/(м·К); A, m — коэффициенты аппроксимации q = A·v^m (табл. 9.1). */
    public static final SteelGrade ST3SP  = new SteelGrade("Ст3сп", 0.15, 27.0, 953.0, 1.0 / 3.0);
    public static final SteelGrade STEEL35 = new SteelGrade("Сталь 35", 0.35, 27.0, 953.0, 1.0 / 3.0);

    /** Температура ликвидус, К — уравнение (9.21). */
    public double liquidusK()      { return 1812.0 - 80.0 * carbonPercent; }

    /** Температура солидус, К — уравнение (9.22). */
    public double solidusK()       { return 1787.0 - 156.0 * carbonPercent; }

    /** Температура кристаллизации, °C: Ткр = Тл при C < 0,2 %, иначе 0,96·Тл (разд. 9.2.1). */
    public double crystallizationC() {
        double tlC = liquidusK() - 273.15;
        return carbonPercent < 0.2 ? tlC : 0.96 * tlC;
    }
}
