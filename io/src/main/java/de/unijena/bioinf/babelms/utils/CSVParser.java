package de.unijena.bioinf.babelms.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple CSV Parser
 */
public class CSVParser {

    private final char Separator;
    private final Character QuoteChar;

    private final Character EscapeChar;

    private final boolean ForceParsing;

    public CSVParser(char separator) {
        this(separator, null, null, false);
    }

    public CSVParser(char separator, Character quoteChar, Character escapeChar, boolean forceParsing) {
        Separator = separator;
        QuoteChar = quoteChar;
        EscapeChar = escapeChar;
        ForceParsing = forceParsing;
    }

    public Iterator<String[]> parse(BufferedReader reader) throws IOException {
        return new RowIterator(reader, Separator, QuoteChar, EscapeChar);
    }

    public class RowIterator implements Iterator<String[]>{

        private final BufferedReader reader;
        private String line;
        private final char separator;
        private final Character quoteChar;
        private final Character escapeChar;

        public RowIterator(BufferedReader reader, char separator, Character quoteChar, Character escapeChar) throws IOException {
            this.reader = reader;
            this.line = reader.readLine();
            this.separator = separator;
            this.quoteChar = quoteChar;
            this.escapeChar = escapeChar;
        }

        @Override
        public boolean hasNext() {
            return line != null;
        }

        @Override
        public String[] next() {
            String currentLine = line;
            try {
                line = reader.readLine();
                String[] columns =  split(currentLine);
                return columns;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String[] split(String currentLine) {
            List<String> columns = new ArrayList<>();
            StringBuilder column = new StringBuilder();
            boolean inQuoteMode = false;
            for (int i = 0; i < currentLine.length(); i++) {
                 char c = currentLine.charAt(i);
                if ((escapeChar != null) && c == escapeChar){
                    if (i == currentLine.length() -1){
                        if (((escapeChar == quoteChar) && inQuoteMode) || ForceParsing) {
                            columns.add(column.toString());
                            return columns.toArray(new String[0]);
                        } else {
                            throw new RuntimeException("Line ends with escape character: "+currentLine);
                        }
                    }
                    char next_c = currentLine.charAt(i+1);
                    if (escapeChar == quoteChar) {
                        //for some weird files double quoteChar means escaped quoteChar
                        if (next_c == quoteChar) {
                            //only escape quoteChar, nothing else!
                            column.append(next_c);
                            ++i;
                        } else {
                            inQuoteMode = !inQuoteMode;
                        }
                    } else {
                        column.append(next_c);
                        ++i;
                    }
                } else if ((quoteChar != null) && c == quoteChar){
                    inQuoteMode = !inQuoteMode;
                } else if (inQuoteMode) {
                    column.append(c);
                } else if (c == separator){
                    //start new column
                    columns.add(column.toString());
                    column = new StringBuilder();
                } else {
                    column.append(c);
                }
            }
            columns.add(column.toString());
            return columns.toArray(new String[0]);
        }
    }
}
