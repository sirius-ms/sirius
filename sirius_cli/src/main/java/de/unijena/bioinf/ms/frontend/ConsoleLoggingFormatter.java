package de.unijena.bioinf.ms.frontend;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class ConsoleLoggingFormatter extends Formatter{
    private final SimpleFormatter formatter = new SimpleFormatter();
    public ConsoleLoggingFormatter() {
        super();
    }

    @Override
    public String format(LogRecord logRecord) {
        if (logRecord.getLevel() == Level.SEVERE) return formatter.format(logRecord);
        else return logRecord.getLevel().getLocalizedName() + ": " + logRecord.getMessage() + "\n";
    }
}
