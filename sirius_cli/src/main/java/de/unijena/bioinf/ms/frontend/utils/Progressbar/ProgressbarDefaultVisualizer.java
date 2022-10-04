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

    private String printProgress() throws InterruptedException{
        StringBuilder progressbar = new StringBuilder();
        progressbar.append("█".repeat(calculator.getProgress()));
        if (status == 0) progressbar.append("▏");
        else if (status == 1) progressbar.append("▍");
        else if (status == 2) progressbar.append("▋");
        else if (status == 3) progressbar.append("▉");
        while(progressbar.length() < calculator.getMaxsize()) progressbar.append(" ");
        return ("Progress: ["+progressbar+"]\r");
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

}
