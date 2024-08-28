package de.unijena.bioinf.ms.frontend.subtools.summaries;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class XlsxTableWriter implements SummaryTableWriter {

    public final static String DOUBLE_PATTERN = "0.###";
    public final static String INTEGER_PATTERN = "#";

    private final OutputStream out;
    private final Workbook workBook;
    private final Sheet sheet;
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
        Row r = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = r.createCell(i, CellType.STRING);
            cell.setCellValue(columns.get(i));
        }
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
}
