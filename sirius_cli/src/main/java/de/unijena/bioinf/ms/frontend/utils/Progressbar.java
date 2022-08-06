package de.unijena.bioinf.ms.frontend.utils;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.security.InvalidParameterException;

/**
 * class for a Progressbar instance.
 * Do not print to the given PrintStream while the Progressbar is not finished
 */
public class Progressbar implements Runnable {
    private final Integer DELAY = 500;
    private final Integer UPDATE = 10;
    private Thread thread;
    private Integer currentprogress;
    private final Integer maxprogress;
    private final Integer stepsize;
    private final Integer actualMaxsize;
    private final Integer MAXSIZE = 32;
    private final PrintStream output;

    /**
     * generated a Progressbar instance. Please use the println function of this Progressbar instance
     * to send additional Messages via the PrintStream
     * @param steps the amount of different Positions in the Progressbar
     */
    public Progressbar(@NotNull Integer steps,@NotNull PrintStream output) {
        this.maxprogress = steps;
        this.output = output;
        this.currentprogress = 0;
        this.stepsize = calculateStepsize(steps);
        this.actualMaxsize = stepsize*maxprogress;
    }

    /**
     * starts the Progressbar
     */
    public void start() {
        if (this.thread == null) this.thread = new Thread(this);
        thread.start();
    }

    /**
     * interrupts the Progressbar to enable printing to the PrintStream
     */
    public void interrupt() {
        if (!thread.isInterrupted()) thread.interrupt();
    }

    /**
     * run method for printing the progressbar to the given PrintStream. Called via internal Thread
     */
    public void run() {
        try {
            int status = -1;
            while ((!thread.isInterrupted()) && currentprogress < maxprogress) {
                for(int i=0; i<this.DELAY/this.UPDATE; i++) {
                    output.print(printProgress(status));
                    Thread.sleep(this.UPDATE);
                }
                status = (status+1) % 4;
            }
            output.println(printProgress(-1));
        }
        catch(InterruptedException ignored) {}
    }

    /**
     * calculates the size of the bar for the given Stepsize
     * @param steps the maximum amount of different positions
     * @return the size of the bar for any stepsize
     */
    private int calculateStepsize(@NotNull Integer steps) {
        if (steps == 0) throw new InvalidParameterException("cannot have Stepsize 0");
        if (steps > MAXSIZE) return 1;
        return MAXSIZE/steps;
    }

    /**
     * prints the current Progress of the Progressbar
     * @return the current Progressbar
     */
    @NotNull
    private String printProgress(int status) {
        StringBuilder progressbar = new StringBuilder();
        progressbar.append("█".repeat(stepsize*currentprogress));
        if (status == 0) progressbar.append("▏");
        else if (status == 1) progressbar.append("▍");
        else if (status == 2) progressbar.append("▋");
        else if (status == 3) progressbar.append("▉");
        while(progressbar.length() < actualMaxsize) progressbar.append(" ");
        return ("Progress: ["+progressbar+"]\r");
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
     * increases the bar by 1 step
     */
    public void increaseProgress() {
        if (currentprogress.equals(maxprogress)) throw new IndexOutOfBoundsException("Progressbar limit reached!");
        this.currentprogress++;
    }

    /**
     * decreases the bar by 1 step
     */
    public void decreaseProgress() {
        if (currentprogress == 0) throw new IndexOutOfBoundsException("Progressbar limit reached!");
        this.currentprogress--;
    }
}
