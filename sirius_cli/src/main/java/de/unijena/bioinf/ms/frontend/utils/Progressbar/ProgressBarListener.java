package de.unijena.bioinf.ms.frontend.utils.Progressbar;

import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import lombok.Setter;

import java.util.Objects;

public class ProgressBarListener implements JobProgressEventListener {

    public static int TERMINAL_WIDTH = 79;
    public static int PROGRESS_BAR_WIDTH = 25;

    @Setter
    private String leadingText = "Progress: ";

    @Override
    public void progressChanged(JobProgressEvent e) {
        System.out.print(getProgressString(e) + '\r');
        if (e.isDone()) {
            System.out.println();
        }
    }

    private String getProgressString(JobProgressEvent e) {
        String s = leadingText
                + "["
                + ProgressbarDefaultVisualizer.formatProgressBarUnicode(e.getProgress(), e.getMaxValue(), PROGRESS_BAR_WIDTH)
                + "] "
                + Objects.requireNonNullElse(e.getMessage(), e.isDone() ? "DONE" : "")
                + " ".repeat(TERMINAL_WIDTH);
        return s.substring(0, TERMINAL_WIDTH);
    }
}
