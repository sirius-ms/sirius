package de.unijena.bioinf.ms.frontend.subtools.summaries;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class XlsxTableWriter implements SummaryTableWriter {

    public final static String DOUBLE_PATTERN = "0.###";
    public final static String INTEGER_PATTERN = "0";

    private final OutputStream out;
    private final SXSSFWorkbook workBook;
    private final SXSSFSheet sheet;
    private CellStyle doubleStyle;
    private CellStyle integerStyle;

    public XlsxTableWriter(Path location, String filenameWithoutExtension) throws IOException {
        out = Files.newOutputStream(location.resolve(filenameWithoutExtension + ".xlsx"));

        workBook = new SXSSFWorkbook();
        createNumericStyles();
        sheet = workBook.createSheet();
    }

    @Override
    public void writeHeader(List<String> columns) throws IOException {
        CellStyle style = createHeaderStyle();
        sheet.trackAllColumnsForAutoSizing();
        Row r = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = r.createCell(i, CellType.STRING);
            cell.setCellValue(columns.get(i));
            cell.setCellStyle(style);
            sheet.autoSizeColumn(i);
        }
        sheet.createFreezePane(0, 1);
        sheet.untrackAllColumnsForAutoSizing();
    }

    @Override
    public void writeRow(List<Object> row) {
        Row r = sheet.createRow(sheet.getLastRowNum() + 1);
        for (int i = 0; i < row.size(); i++) {
            Object val = row.get(i);

            if (val == null || val.equals(Double.NaN)) {
                continue;
            }
            if (val.equals(Double.NEGATIVE_INFINITY) || val.equals(Double.POSITIVE_INFINITY)) {
                val = val.toString();  // For consistency with tsv
            }
            if (val instanceof String s) {
                r.createCell(i, CellType.STRING).setCellValue(s);
            }
            else if (val instanceof Number n) {
                Cell cell = r.createCell(i, CellType.NUMERIC);
                cell.setCellValue(n.doubleValue());
                cell.setCellStyle(val instanceof Double ? doubleStyle : integerStyle);
            } else {
                log.warn("XLSX writer encountered a value of an unexpected type {} {}", val, val.getClass());
                r.createCell(i, CellType.STRING).setCellValue(val.toString());
            }
        }
    }

    @Override
    public void flush() throws IOException {
        workBook.write(out);
    }

    @Override
    public void close() throws Exception {
        if (out != null) {
            out.close();
        }
        if (workBook != null) {
            workBook.close();
        }
    }

    private void createNumericStyles() {
        DataFormat format = workBook.createDataFormat();
        CellStyle dStyle = workBook.createCellStyle();
        dStyle.setDataFormat(format.getFormat(DOUBLE_PATTERN));
        doubleStyle = dStyle;

        CellStyle iStyle = workBook.createCellStyle();
        iStyle.setDataFormat(format.getFormat(INTEGER_PATTERN));
        integerStyle = iStyle;
    }

    private CellStyle createHeaderStyle() {
        CellStyle style = workBook.createCellStyle();

        XSSFColor bgColor = new XSSFColor(new java.awt.Color(245, 187, 201), new DefaultIndexedColorMap());  // TODO streamline with other colors
        style.setFillForegroundColor(bgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workBook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

}
