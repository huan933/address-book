package com.fzu.addressbook.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 读写：与前端 {@code HEADERS} 完全对齐的 12 列契约。
 *
 * <p>列顺序：姓名 / 单位 / 收藏 / 备注 / 电话1-3 / 邮箱1-2 / 社交1-2 / 地址。
 * 导出与导入共用这一份定义，保证「导出再导入」能原样回灌。</p>
 */
public final class ExcelSupport {

    public static final String[] HEADERS = {
            "姓名", "单位", "收藏", "备注",
            "电话1", "电话2", "电话3",
            "邮箱1", "邮箱2",
            "社交1", "社交2",
            "地址"
    };

    private ExcelSupport() {
    }

    /** 把行数据写成 xlsx 字节流（含表头）。 */
    public static byte[] toXlsx(List<List<String>> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("通讯录");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, 18 * 256);
            }
            int r = 1;
            for (List<String> data : rows) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < HEADERS.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(c < data.size() && data.get(c) != null ? data.get(c) : "");
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 读取 xlsx / csv 为二维字符串表；不支持的文件类型抛 IllegalArgumentException。 */
    public static List<List<String>> read(InputStream in, String filename) throws IOException {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return readCsv(in);
        }
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new IllegalArgumentException("只支持 .xlsx / .xls / .csv 文件");
        }
        List<List<String>> table = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < HEADERS.length; c++) {
                    cells.add(cellText(row.getCell(c)));
                }
                table.add(cells);
            }
        }
        return table;
    }

    private static String cellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.floor(d) && !Double.isInfinite(d)
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case BOOLEAN -> cell.getBooleanCellValue() ? "是" : "否";
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /** 极简 CSV 解析：支持双引号包裹与引号内逗号。 */
    private static List<List<String>> readCsv(InputStream in) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }
            switch (ch) {
                case '"' -> quoted = true;
                case ',' -> {
                    row.add(field.toString());
                    field.setLength(0);
                }
                case '\r' -> {
                    // 交给 \n 处理
                }
                case '\n' -> {
                    row.add(field.toString());
                    field.setLength(0);
                    table.add(row);
                    row = new ArrayList<>();
                }
                default -> field.append(ch);
            }
        }
        if (!field.isEmpty() || !row.isEmpty()) {
            row.add(field.toString());
            table.add(row);
        }
        return table;
    }
}
