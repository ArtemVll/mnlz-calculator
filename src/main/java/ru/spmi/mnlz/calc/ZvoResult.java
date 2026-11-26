package ru.spmi.mnlz.calc;

/** Результаты теплового расчёта ЗВО (разд. 10). */
public record ZvoResult(double billetRateKgs,        // Gз, кг/с
                        double heatContentIn,        // iн, кДж/кг (10.4)
                        double heatContentOut,       // iк, кДж/кг (10.5)
                        double avgSurfaceTempC,      // Тп ср (10.6)
                        double perimeterM,           // П (10.8)
                        double zvoLengthM,           // lЗВО (10.9)/(10.10)
                        double surfaceM2,            // F (10.7)
                        double heatTransferCoef,     // αΣ (10.3)
                        double specificWater,        // g, м3/(м2*ч) (10.11)
                        double waterFlow,            // Gв, м3/ч (10.12)
                        double waterFlowMinorR,      // по малому радиусу (10.13)
                        double waterFlowMajorR,      // по большому радиусу (10.14)
                        ZvoSection[] sections,       // участки ЗВО (10.18)-(10.19)
                        int totalNozzles,            // nф (10.16)
                        double nozzleDistanceM) { }  // h (10.17)
