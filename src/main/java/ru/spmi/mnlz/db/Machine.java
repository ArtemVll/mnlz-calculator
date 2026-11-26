package ru.spmi.mnlz.db;

/** Одна строка витрины оборудования МНЛЗ. */
public record Machine(String name, String type, double radiusM, int streams, int ladleT) { }
