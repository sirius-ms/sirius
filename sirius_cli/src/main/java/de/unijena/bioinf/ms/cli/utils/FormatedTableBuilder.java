package de.unijena.bioinf.ms.cli.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FormatedTableBuilder {

    private String paddingChar = "-";
    private int minSpacing = 5;

    private Table table;

    public FormatedTableBuilder() {
        table = new Table();
    }

    /**
     * set the character that is used for values that don't exist.
     *
     * @param paddingChar padding char
     */
    public void setPaddingChar(String paddingChar) {
        this.paddingChar = paddingChar;
    }

    /**
     * the minimum amount of spaces between columns in the formated output.
     *
     * Must be at least 0, but a minimum of at least 1 is suggested.
     *
     * @param minSpacing minimum of spaces (must be at least 0)
     */
    public void setMinSpacing(int minSpacing) {
        if (minSpacing < 0) {
            throw new IllegalArgumentException("min spacing must be at least 0");
        }
        this.minSpacing = minSpacing;
    }

    public interface Formater<T> {
        String[] format(T object);
    }

    public <T> FormatedTableBuilder add(List<T> objects, Formater<T> formater) {
        objects.forEach((T object) -> addLine(formater.format(object)));
        return this;
    }

    public FormatedTableBuilder addLine(int position, List<String> lineContent) {
        table.addLine(position, new Line(lineContent));
        return this;
    }

    public FormatedTableBuilder addLine(List<String> lineContent) {
        return addLine(table.lineCount(), lineContent);
    }

    public FormatedTableBuilder addLine(String... lineContent) {
        return addLine(table.lineCount(), new ArrayList<>(Arrays.asList(lineContent)));
    }

    public FormatedTableBuilder addLine(int position, String... lineContent) {
        return addLine(position, new ArrayList<>(Arrays.asList(lineContent)));
    }

    public FormatedTableBuilder addColumn(int position, List<String> columnContent) {
        table.addColumn(position, columnContent);
        return this;
    }

    public FormatedTableBuilder addColumn(List<String> columnContent) {
        return addColumn(table.columnCount(), columnContent);
    }

    public FormatedTableBuilder addColumn(String... columnContent) {
        return addColumn(table.columnCount(), new ArrayList<>(Arrays.asList(columnContent)));
    }

    public FormatedTableBuilder addColumn(int position, String... columnContent) {
        return addColumn(position, new ArrayList<>(Arrays.asList(columnContent)));
    }

    /**
     * sets the value at the given column and row to the given content.
     *
     * If the value doesn't exist already, an IllegalArgumentException will be
     * thrown.
     *
     * @param column column
     * @param row row
     * @param value value
     * @return this builder
     */
    public FormatedTableBuilder set(int column, int row, String value) {
        table.set(column, row, value);
        return this;
    }

    /**
     * passes the formated string to consumer.
     *
     * To get the formated string, use format.
     *
     * @param consumer consumer
     */
    public void output(Consumer<String> consumer) {
        consumer.accept(format());
    }

    /**
     * formats all given content so it presents as a well aligned table.
     *
     * @return formated content
     */
    public String format() {
        List<Integer> maxContentLength = getMaxLengths();
        List<String> spaces = getSpaces(maxContentLength);

        // calculate stringbuilder size
        int rowwidth = maxContentLength.stream().mapToInt(i -> i).sum();
        rowwidth += (table.columnCount() - 1) * minSpacing + 1; // spaces between columns and a newline
        int charCount = rowwidth * table.lineCount();

        StringBuilder out = new StringBuilder(charCount);
        for (Line line : table.lines) {
            for (int columnIndex = 0; columnIndex < line.size(); columnIndex++) {
                String spacesForCurrentColumn = spaces.get(columnIndex);
                String column = line.get(columnIndex);
                out.append(column);
                out.append(spacesForCurrentColumn
                        .substring(0,
                                spacesForCurrentColumn.length()
                                        - column.length())); // align
            }
            out.append("\n");
        }
        return out.toString();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        table.lines.forEach(line -> {
            line.forEach(
                    field -> out.append(field).append("            "));
            out.append("\n");
        });
        return out.toString();
    }

    /**
     * returns a list of spaces of the given lengths.
     *
     * @param lengths lengths
     * @return list of spaces
     */
    private List<String> getSpaces(List<Integer> lengths) {
        List<String> spaces = new ArrayList<>();
        lengths.forEach(length -> spaces.add(getSpaces(length)));
        return spaces;
    }

    /**
     * returns n spaces.
     *
     * @param n number of spaces
     * @return n spaces
     */
    private static String getSpaces(int n) {
        StringBuilder spaces = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    /**
     * returns a list containing the length of the longest string of each
     * column.
     *
     * @return lengths
     */
    private List<Integer> getMaxLengths() {
        List<Integer> maxContentLength = new ArrayList<>();
        for (int i = 0; i < table.columnCount(); i++) {
            maxContentLength.add(0); // init max length with 0
        }

        for (Line line : table.lines) {
            for (int columnIndex = 0; columnIndex < table.columnCount(); columnIndex++) {
                String column = line.get(columnIndex);
                if (column.length() >= maxContentLength.get(columnIndex)) {
                    maxContentLength.set(columnIndex, column.length() + minSpacing);
                }
            }
        }
        return maxContentLength ;
    }

    private class Table {

        private List<Line> lines;

        public Table() {
            lines = new ArrayList<>();
        }

        public int lineCount() {
            return lines.size();
        }

        public int columnCount() {
            if (lines.isEmpty()) {
                return 0;
            }
            return lines.get(0).size(); // all lines are always the same length because of padding
        }

        public void set(int column, int row, String value) {
            if (row < 0 || row > lineCount()) {
                throw new IllegalArgumentException("row doesn't exist");
            }
            Line line = lines.get(row);
            if (column < 0 || column > columnCount()) {
                throw new IllegalArgumentException("column doesn't exist");
            }
            line.set(column, value);
        }

        public void addColumn(int position, List<String> columnContent) {
            if (position < 0) {
                throw new IllegalArgumentException("Column position is negative");
            }

            // if the new position is outside the current content, extend short lines to position
            if (position > columnCount()) {
                padLinesTo(position);
            }

            // add new lines for content of this column in case the column is too long
            while (lines.size() < columnContent.size()) {
                addLinePaddedTo(position);
            }

            shiftAllAt(position, lines); // make space for new column
            for (int lineIndex = 0; lineIndex < columnContent.size(); lineIndex++) { // add new column
                set(position, lineIndex, columnContent.get(lineIndex));
            }

            if (columnContent.size() > columnCount()) {
                padLinesTo(table.columnCount());
            }
        }

        public void addLine(int position, List<String> columnContent) {
            Line newLine = new Line(columnContent);
            if (position < 0) {
                throw new IllegalArgumentException("Line position is negative");
            }

            // add elements to this line if it is too short
            newLine.padTo(columnCount());

            if (newLine.size() > columnCount()) { // add elements to all other lines if bew line is the new longest line
                padLinesTo(newLine.size()); // padd to new longest line
            }

            while (lines.size() < position) { // add new padded empty lines if new line is outside field
                addLinePaddedTo(columnCount());
            }
            lines.add(position, newLine); // add actual line
        }

        /**
         * adds a new line to the end of the content, padded to the given
         * position.
         *
         * @param padTo padTo
         */
        private void addLinePaddedTo(int padTo) {
            Line emptyLine = new Line();
            emptyLine.padTo(padTo);
            lines.add(emptyLine);
        }

        /**
         * adds padding char to all lines until they are the given length.
         *
         * @param padTo padTo
         */
        private void padLinesTo(int padTo) {
            lines.forEach(line -> line.padTo(padTo));
        }

        /**
         * shifts all given lines at given position one entry to the right.
         *
         * The padding character will be inserted at the new position.
         *
         * @param position position
         * @param list list
         */
        private void shiftAllAt(int position, List<Line> list) {
            list.forEach(line -> line.shiftAt(position));
        }
    }

    private class Line extends ArrayList<String> {

        public Line() {
            super();
        }

        public Line(List<String> line) {
            super(line);
        }

        /**
         * add padding char to this line until its length is the given max.
         *
         * @param max max
         */
        private void padTo(int max) {
            while (this.size() < max) {
                this.add(paddingChar);
            }
        }

        /**
         * inserts the padding char at the given position, thus shifting the
         * rest of the line one position to the right.
         *
         * @param position position
         */
        private void shiftAt(int position) {
            this.add(position, paddingChar);
        }
    }
}
