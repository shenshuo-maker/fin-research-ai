package com.finresearch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatasetProcessingService {

    private final ObjectMapper objectMapper;

    public List<String[]> parseTabular(byte[] bytes, String fileName) throws Exception {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return parseCsv(bytes);
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return parseExcel(bytes);
        }
        throw new IllegalArgumentException("仅支持 CSV 或 Excel");
    }

    private List<String[]> parseCsv(byte[] bytes) throws Exception {
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<String> headers = parser.getHeaderNames();
            List<String[]> rows = new ArrayList<>();
            rows.add(headers.toArray(new String[0]));
            for (CSVRecord r : parser) {
                String[] line = new String[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    line[i] = r.get(i);
                }
                rows.add(line);
            }
            return rows;
        }
    }

    private List<String[]> parseExcel(byte[] bytes) throws Exception {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            List<String[]> rows = new ArrayList<>();
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                short last = row.getLastCellNum();
                if (last < 0) {
                    continue;
                }
                String[] line = new String[last];
                for (int c = 0; c < last; c++) {
                    Cell cell = row.getCell(c);
                    line[c] = cell == null ? "" : cellToString(cell);
                }
                rows.add(line);
            }
            return rows;
        }
    }

    private static String cellToString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellType() == CellType.NUMERIC
                    ? String.valueOf(cell.getNumericCellValue())
                    : cell.getStringCellValue();
            default -> "";
        };
    }

    public ObjectNode cleanReport(List<String[]> rows) {
        ObjectNode root = objectMapper.createObjectNode();
        if (rows.isEmpty()) {
            root.put("error", "空文件");
            return root;
        }
        String[] header = rows.get(0);
        int n = rows.size() - 1;
        root.put("rowCount", n);
        root.put("colCount", header.length);

        Set<String> seenRows = new HashSet<>();
        int dupRows = 0;
        ArrayNode missing = objectMapper.createArrayNode();
        ArrayNode outliers = objectMapper.createArrayNode();
        ArrayNode invalidFinance = objectMapper.createArrayNode();

        Map<Integer, Integer> missCol = new HashMap<>();
        for (int c = 0; c < header.length; c++) {
            missCol.put(c, 0);
        }

        for (int i = 1; i < rows.size(); i++) {
            String[] line = rows.get(i);
            String key = String.join("|", line);
            if (!seenRows.add(key)) {
                dupRows++;
            }
            for (int c = 0; c < header.length; c++) {
                String v = c < line.length ? line[c] : "";
                if (v == null || v.isBlank()) {
                    missCol.merge(c, 1, Integer::sum);
                }
            }
        }

        for (int c = 0; c < header.length; c++) {
            int m = missCol.get(c);
            if (m > 0) {
                ObjectNode o = objectMapper.createObjectNode();
                o.put("column", header[c]);
                o.put("missingCount", m);
                o.put("missingRate", n == 0 ? 0 : Math.round(m * 1000.0 / n) / 10.0);
                missing.add(o);
            }
        }

        // 数值列简单 IQR 异常
        for (int c = 0; c < header.length; c++) {
            DescriptiveStatistics ds = new DescriptiveStatistics();
            for (int i = 1; i < rows.size(); i++) {
                String[] line = rows.get(i);
                String v = c < line.length ? line[c] : "";
                try {
                    if (v != null && !v.isBlank()) {
                        ds.addValue(Double.parseDouble(v.trim()));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (ds.getN() > 4) {
                double q1 = ds.getPercentile(25);
                double q3 = ds.getPercentile(75);
                double iqr = q3 - q1;
                double low = q1 - 1.5 * iqr;
                double high = q3 + 1.5 * iqr;
                int cnt = 0;
                for (int i = 1; i < rows.size(); i++) {
                    String[] line = rows.get(i);
                    String v = c < line.length ? line[c] : "";
                    try {
                        double x = Double.parseDouble(v.trim());
                        if (x < low || x > high) {
                            cnt++;
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (cnt > 0) {
                    ObjectNode o = objectMapper.createObjectNode();
                    o.put("column", header[c]);
                    o.put("outlierCount", cnt);
                    outliers.add(o);
                }
            }
        }

        // CSMAR 风格日期列 yyyy-mm-dd；Akshare 风格代码 6 位
        for (int c = 0; c < header.length; c++) {
            String col = header[c].toLowerCase(Locale.ROOT);
            if (col.contains("date") || col.contains("日期")) {
                int bad = 0;
                for (int i = 1; i < Math.min(rows.size(), 500); i++) {
                    String[] line = rows.get(i);
                    String v = c < line.length ? line[c] : "";
                    if (v != null && !v.isBlank() && !v.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        bad++;
                    }
                }
                if (bad > 0) {
                    ObjectNode o = objectMapper.createObjectNode();
                    o.put("column", header[c]);
                    o.put("csmarDateViolationsSample", bad);
                    invalidFinance.add(o);
                }
            }
            if (col.contains("code") || col.contains("代码")) {
                int bad = 0;
                for (int i = 1; i < Math.min(rows.size(), 500); i++) {
                    String[] line = rows.get(i);
                    String v = c < line.length ? line[c] : "";
                    if (v != null && !v.isBlank() && !v.matches("\\d{6}")) {
                        bad++;
                    }
                }
                if (bad > 0) {
                    ObjectNode o = objectMapper.createObjectNode();
                    o.put("column", header[c]);
                    o.put("akshareCodeViolationsSample", bad);
                    invalidFinance.add(o);
                }
            }
        }

        root.set("missingByColumn", missing);
        root.set("outliers", outliers);
        root.set("financeFieldChecks", invalidFinance);
        root.put("duplicateRows", dupRows);
        root.put("duplicateKeysEstimate", dupRows > 0);
        return root;
    }

    /** 删除完全重复行、填充空值为 NA */
    public List<String[]> applyClean(List<String[]> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String[]> out = new ArrayList<>();
        out.add(rows.get(0));
        for (int i = 1; i < rows.size(); i++) {
            String[] line = rows.get(i);
            String key = String.join("|", line);
            if (seen.add(key)) {
                String[] cp = Arrays.copyOf(line, line.length);
                for (int j = 0; j < cp.length; j++) {
                    if (cp[j] == null || cp[j].isBlank()) {
                        cp[j] = "NA";
                    }
                }
                out.add(cp);
            }
        }
        return out;
    }

    public ObjectNode descriptive(List<String[]> rows) {
        ObjectNode root = objectMapper.createObjectNode();
        if (rows.size() < 2) {
            return root;
        }
        String[] h = rows.get(0);
        ArrayNode arr = objectMapper.createArrayNode();
        for (int c = 0; c < h.length; c++) {
            DescriptiveStatistics ds = new DescriptiveStatistics();
            for (int i = 1; i < rows.size(); i++) {
                String[] line = rows.get(i);
                String v = c < line.length ? line[c] : "";
                try {
                    if (v != null && !v.isBlank() && !"NA".equals(v)) {
                        ds.addValue(Double.parseDouble(v.trim()));
                    }
                } catch (Exception ignored) {
                }
            }
            if (ds.getN() > 0) {
                ObjectNode o = objectMapper.createObjectNode();
                o.put("column", h[c]);
                o.put("n", ds.getN());
                o.put("mean", ds.getMean());
                o.put("std", ds.getStandardDeviation());
                o.put("min", ds.getMin());
                o.put("max", ds.getMax());
                arr.add(o);
            }
        }
        root.set("columns", arr);
        return root;
    }

    public ObjectNode ttest(List<String[]> rows, String valueCol, String groupCol) {
        ObjectNode root = objectMapper.createObjectNode();
        if (rows.size() < 2) {
            root.put("error", "数据不足");
            return root;
        }
        String[] h = rows.get(0);
        int vi = indexOf(h, valueCol);
        int gi = indexOf(h, groupCol);
        if (vi < 0 || gi < 0) {
            root.put("error", "列名不存在");
            return root;
        }
        List<Double> g0 = new ArrayList<>();
        List<Double> g1 = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] line = rows.get(i);
            try {
                double val = Double.parseDouble(line[vi].trim());
                String g = line[gi].trim();
                if ("0".equals(g) || "A".equalsIgnoreCase(g) || "对照".equals(g)) {
                    g0.add(val);
                } else {
                    g1.add(val);
                }
            } catch (Exception ignored) {
            }
        }
        if (g0.size() < 2 || g1.size() < 2) {
            root.put("error", "分组样本不足（请用 group 列：0/1 或 A/B）");
            return root;
        }
        TTest tt = new TTest();
        double[] a = g0.stream().mapToDouble(Double::doubleValue).toArray();
        double[] b = g1.stream().mapToDouble(Double::doubleValue).toArray();
        double p = tt.tTest(a, b);
        root.put("pValue", p);
        root.put("meanGroup0", Arrays.stream(a).average().orElse(0));
        root.put("meanGroup1", Arrays.stream(b).average().orElse(0));
        return root;
    }

    public ObjectNode ols(List<String[]> rows, String yCol, List<String> xCols) {
        ObjectNode root = objectMapper.createObjectNode();
        if (rows.size() < 3) {
            root.put("error", "数据不足");
            return root;
        }
        String[] h = rows.get(0);
        int yi = indexOf(h, yCol);
        int[] xi = xCols.stream().mapToInt(x -> indexOf(h, x)).toArray();
        if (yi < 0 || Arrays.stream(xi).anyMatch(v -> v < 0)) {
            root.put("error", "列名不存在");
            return root;
        }
        int k = xi.length;
        List<double[]> xRows = new ArrayList<>();
        List<Double> yVals = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] line = rows.get(i);
            try {
                double y = Double.parseDouble(line[yi].trim());
                double[] row = new double[k];
                for (int j = 0; j < k; j++) {
                    row[j] = Double.parseDouble(line[xi[j]].trim());
                }
                yVals.add(y);
                xRows.add(row);
            } catch (Exception ignored) {
            }
        }
        if (yVals.size() < k + 2) {
            root.put("error", "有效样本过少");
            return root;
        }
        double[] yv = yVals.stream().mapToDouble(Double::doubleValue).toArray();
        double[][] xmv = xRows.toArray(new double[0][]);
        OLSMultipleLinearRegression reg = new OLSMultipleLinearRegression();
        reg.newSampleData(yv, xmv);
        double[] beta = reg.estimateRegressionParameters();
        root.put("rSquared", reg.calculateRSquared());
        ArrayNode names = objectMapper.createArrayNode();
        names.add("const");
        for (String xc : xCols) {
            names.add(xc);
        }
        root.set("paramNames", names);
        ArrayNode betas = objectMapper.createArrayNode();
        for (double b : beta) {
            betas.add(b);
        }
        root.set("coefficients", betas);
        return root;
    }

    public ArrayNode chartSpecScatter(List<String[]> rows, String xCol, String yCol) {
        ArrayNode pts = objectMapper.createArrayNode();
        if (rows.size() < 2) {
            return pts;
        }
        String[] h = rows.get(0);
        int xi = indexOf(h, xCol);
        int yi = indexOf(h, yCol);
        if (xi < 0 || yi < 0) {
            return pts;
        }
        int limit = 500;
        for (int i = 1; i < rows.size() && i <= limit; i++) {
            try {
                double x = Double.parseDouble(rows.get(i)[xi].trim());
                double y = Double.parseDouble(rows.get(i)[yi].trim());
                ObjectNode p = objectMapper.createObjectNode();
                p.put("x", x);
                p.put("y", y);
                pts.add(p);
            } catch (Exception ignored) {
            }
        }
        return pts;
    }

    private static int indexOf(String[] h, String name) {
        for (int i = 0; i < h.length; i++) {
            if (h[i] != null && h[i].equalsIgnoreCase(name.trim())) {
                return i;
            }
        }
        return -1;
    }
}
