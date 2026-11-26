package ru.spmi.mnlz.db;

import java.util.Map;

/** Одна запись истории: исходные данные и результаты одного расчёта. */
public record HistoryEntry(int id, String steel, double width, double thickness,
                           double speed, String created,
                           Map<String, Double> inputs, Map<String, Double> results) { }
