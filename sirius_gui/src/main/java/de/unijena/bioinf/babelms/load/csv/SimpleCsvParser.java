package de.unijena.bioinf.babelms.load.csv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimpleCsvParser {
    public char separator;
    public char quotationChar;
    protected ArrayList<String> listBuffer;
    protected StringBuilder stringBuffer;

    public static SimpleCsvParser guessSeparator(List<String> preview) {
        if (!preview.isEmpty()) {
            final char[] trySeps = new char[]{'\t',',',';',':',' '};
            final char[] tryQuots = new char[]{'"', '\''};
            for (char quot : tryQuots) {
                eachSeparator:
                for (char sep : trySeps) {
                    final SimpleCsvParser p = new SimpleCsvParser(sep, quot);
                    Iterator<String> l = preview.iterator();
                    int cols = p.parseLine(l.next()).length;
                    if (cols<=1) continue eachSeparator;
                    while (l.hasNext()) {
                        if (p.parseLine(l.next()).length != cols) continue eachSeparator;
                    }
                    return p;
                }
            }
        }
        return new SimpleCsvParser();
    }

    public SimpleCsvParser() {
        this('\t','"');
    }

    public SimpleCsvParser(char sep, char quot) {
        this.separator = sep;
        this.quotationChar = quot;
        this.listBuffer = new ArrayList<>();
        this.stringBuffer = new StringBuilder();
    }

    public String[] parseLine(CharSequence line) {
        listBuffer.clear();
        int beginEscape=-1;
        for (int i=0, n = line.length(); i < n; ++i) {
            final char c = line.charAt(i);
            if (c == quotationChar) {
                if (beginEscape<0) beginEscape = i;
                else if (beginEscape-i <= 1) {
                    stringBuffer.append(quotationChar);
                } else {
                    stringBuffer.append(line.subSequence(beginEscape+1, i-1));
                    beginEscape = -1;
                }
            } else if (c == separator) {
                if (stringBuffer.length()==1 && stringBuffer.charAt(0)==quotationChar)
                    listBuffer.add("");
                else
                    listBuffer.add(stringBuffer.toString());
                stringBuffer.delete(0,stringBuffer.length());
            } else {
                stringBuffer.append(c);
            }
        }
        listBuffer.add(stringBuffer.toString());
        stringBuffer.delete(0,stringBuffer.length());
        return listBuffer.toArray(new String[listBuffer.size()]);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleCsvParser that = (SimpleCsvParser) o;

        if (separator != that.separator) return false;
        return quotationChar == that.quotationChar;
    }

    @Override
    public int hashCode() {
        int result = (int) separator;
        result = 31 * result + (int) quotationChar;
        return result;
    }
}