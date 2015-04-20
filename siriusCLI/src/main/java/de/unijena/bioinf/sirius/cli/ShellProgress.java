package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.sirius.Progress;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Created by kaidu on 20.04.2015.
 */
public class ShellProgress implements Progress {

    private boolean shellMode;
    private PrintStream writer;

    private static char FRAME = '|';
    private static char UNREACHED = '.';
    private static char REACHED = '=';
    private static int width=20;

    private double max;

    private StringBuilder buffer;

    public ShellProgress(PrintStream writer, boolean shellMode) {
        this.shellMode = shellMode;
        this.writer = writer;
        this.buffer = new StringBuilder(160);

    }

    @Override
    public void init(double maxProgress) {
        if (!shellMode) return;
        clearBuffer();
        draw(0, maxProgress, "start computing");
        this.max = maxProgress;
    }

    @Override
    public void update(double currentProgress, double maxProgress, String value) {
        if (!shellMode) {
            writer.println(value);
        } else {
            clearBuffer();
            draw(currentProgress, maxProgress, value);
        }
    }

    private void draw(double currentProgress, double maxProgress, String value) {

        final int percentage = Math.max(0, Math.min(width, (int) Math.round(width * currentProgress / maxProgress)));
        final int perc100 = percentage*5;

        buffer.append(FRAME);
        for (int k=0; k < width/2; ++k) buffer.append(k >= percentage ? UNREACHED : REACHED);

        buffer.append(String.format(Locale.US, "%3d %%", perc100));
        for (int k=width/2; k < width; ++k) buffer.append(k >= percentage ? UNREACHED : REACHED);
        buffer.append(FRAME);
        buffer.append('\n');
        if (value.length() < buffer.length()) {
            final int indent = (buffer.length()-1-value.length())/2;
            for (int i=0; i < indent; ++i) buffer.append(' ');
        }
        buffer.append(value);
        buffer.append('\n');
        writer.print(buffer); writer.flush();
    }

    private void clearBuffer() {
        for (int k=0; k < buffer.length(); ++k) {
            buffer.setCharAt(k, '\b');
        }
        writer.print(buffer);
        writer.flush();
        buffer.delete(0, buffer.length());
    }

    @Override
    public void finished() {
        if (!shellMode) return;
        clearBuffer();
        draw(max, max, "computation finished");
    }

    @Override
    public void info(String message) {
        writer.println();
        writer.println(message);
        buffer.delete(0, buffer.length());
    }
}
