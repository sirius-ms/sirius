package de.unijena.bioinf.ms.frontend.utils.Progressbar;

import java.io.PrintStream;

/**
 * class for a Progressbar instance.
 * Do not print to the given PrintStream while the Progressbar is not finished
 */
public class ProgressbarDefaultVisualizer<ProgressbarCalc extends ProgressbarCalculator> implements Runnable, ProgressVisualizer {
    private final Integer DELAY = 500;
    private final Integer UPDATE = 10;
    private Thread thread;
    private final PrintStream output;
    private Integer status;
    private final ProgressbarCalc calculator;

    public ProgressbarCalculator getCalculator() {
        return calculator;
    }

    /**
     * generated a Progressbar instance. Please use the println function of this Progressbar instance
     * to send additional Messages via the PrintStream
     */
    public ProgressbarDefaultVisualizer(PrintStream output, ProgressbarCalc calculator) {
        this.output = output;
        this.status = 0;
        this.calculator = calculator;
    }
    /**
     * prints the current Progress of the Progressbar
     */
    @Override
    public void visualizeProgress() {
        try {
            while ((!thread.isInterrupted()) && calculator.getProgress() < calculator.getMaxsize()) {
                for (int i = 0; i < this.DELAY / this.UPDATE; i++) {
                    output.print(printProgress());
                    Thread.sleep(this.UPDATE);
                }
                status = (status + 1) % 4;
            }
            this.status = -1;
            output.println(printProgress());
        }
        catch (InterruptedException ignored) {}
    }

    private String printProgress() {
        String progressBar = formatProgressBarUnicode(calculator.getProgress() * 4 + status, calculator.getMaxsize() * 4, calculator.getMaxsize());
        return ("Progress: ["+progressBar+"]\r");
    }

    /**
     * starts the Progressbar
     */
    public void start() {
        if (this.thread == null) this.thread = new Thread(this);
        thread.start();
    }

    /**
     * stops the Progressbar to enable printing to the PrintStream
     */
    @Override
    public void stop() {
            if (!thread.isInterrupted()) thread.interrupt();
    }

    /**
     * run method for printing the progressbar to the given PrintStream. Called via internal Thread
     */
    public void run() {
        visualizeProgress();
    }

    /**
     * Please use this function to print Text while the Progressbar is still running
     * @param message the message to send via the given PrintStream
     */
    public void println(String message) {
        if (!thread.isInterrupted()) output.print(message+"\r\n");
        else output.println(message);
    }

    /**
     * @param progress current progress
     * @param maxProgress max progress
     * @param barWidth length in chars of the resulting progress bar
     * @return a string of length {@code barWidth} representing current progress
     */
    public static String formatProgressBarUnicode(long progress, long maxProgress, int barWidth) {
        if (progress >= maxProgress) {
            return "█".repeat(barWidth);
        }
        int barProgress = (int) Math.round(((progress + 0d)/maxProgress) * (barWidth * 4));
        StringBuilder sb = new StringBuilder();
        int full = barProgress / 4;
        int rem = barProgress % 4;
        sb.append("█".repeat(full));
        if (rem == 1) sb.append("▎");
        if (rem == 2) sb.append("▌");
        if (rem == 3) sb.append("▊");
        if (sb.length() < barWidth) sb.append(" ".repeat(barWidth - sb.length()));
        return sb.toString();
    }
}
