package ru.spmi.mnlz.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.spmi.mnlz.db.HistoryEntry;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Экспорт истории расчётов в файл Excel (.xlsx). */
public final class ExcelExporter {

    private ExcelExporter() { }

    /**
     * Записывает список расчётов в книгу: шапка с базовыми колонками,
     * затем все встречающиеся входные параметры и результаты.
     */
    public static void exportHistory(File file, List<HistoryEntry> entries) throws IOException {
        // Собираем единый список названий параметров по всем записям
        Set<String> inputKeys = new LinkedHashSet<>();
        Set<String> resultKeys = new LinkedHashSet<>();
        for (HistoryEntry h : entries) {
            inputKeys.addAll(h.inputs().keySet());
            resultKeys.addAll(h.results().keySet());
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("История расчётов");

            CellStyle headerStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            String[] base = {"№", "Марка", "Ширина, м", "Толщина, м", "Скорость, м/мин", "Дата"};
            Row head = sheet.createRow(0);
            int c = 0;
            for (String t : base) {
                Cell cell = head.createCell(c++);
                cell.setCellValue(t);
                cell.setCellStyle(headerStyle);
            }
            for (String k : inputKeys) {
                Cell cell = head.createCell(c++);
                cell.setCellValue("Вход: " + k);
                cell.setCellStyle(headerStyle);
            }
            for (String k : resultKeys) {
                Cell cell = head.createCell(c++);
                cell.setCellValue("Рез: " + k);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (HistoryEntry h : entries) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(h.id());
                row.createCell(1).setCellValue(h.steel());
                row.createCell(2).setCellValue(h.width());
                row.createCell(3).setCellValue(h.thickness());
                row.createCell(4).setCellValue(h.speed());
                row.createCell(5).setCellValue(h.created());
                c = base.length;
                for (String k : inputKeys) {
                    Double v = h.inputs().get(k);
                    if (v != null) row.createCell(c).setCellValue(v);
                    c++;
                }
                for (String k : resultKeys) {
                    Double v = h.results().get(k);
                    if (v != null) row.createCell(c).setCellValue(v);
                    c++;
                }
            }

            for (int i = 0; i < head.getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream out = Files.newOutputStream(file.toPath())) {
                wb.write(out);
            }
        }
    }
}
