package de.unijena.bioinf.ms.frontend.utils;

import java.io.PrintStream;

/**
 * class for a Progressbar instance.
 * Do not print to System.out while the Progressbar is not finished
 */
public class Progressbar implements Runnable {
    private Thread thread;
    private Integer currentprogress;
    private final Integer maxprogress;
    private final Integer stepsize;
    private final Integer actualMaxsize;
    private final Integer MAXSIZE = 32;
    private final PrintStream output;

    /**
     * generated a Progressbar instance
     * @param steps the amount of different Positions in the Progressbar
     */
    public Progressbar(Integer steps, PrintStream output) {
        this.maxprogress = steps;
        this.output = output;
        this.currentprogress = 0;
        this.stepsize = calculateStepsize(steps);
        this.actualMaxsize = stepsize*maxprogress;
    }

    /**
     * starts the Progressbar - Please do not write to the Printstream while the Progressbar is not finished
     */
    public void start() {
        if (this.thread == null) this.thread = new Thread(this);
        thread.start();
    }

    /**
     * interrupts the Progressbar to enable printing to the PrintStream
     */
    public void interrupt() {
        thread.interrupt();
    }
    public void run() {
        try {
            while (currentprogress < maxprogress) {
                output.print(printProgress(false));
                Thread.sleep(500);
                output.print(printProgress(true));
                Thread.sleep(500);
            }
            output.println(printProgress(false));
        }
        catch(InterruptedException ignored) {}
    }

    /**
     * calculates the size of the bar for the given Stepsize
     * @param steps the maximum amount of different positions
     * @return the size of the bar for any stepsize
     */
    private int calculateStepsize(Integer steps) {
        for(int i=2; i<=MAXSIZE; i++) {
            if(i*steps > MAXSIZE) return (i-1);
        }
        return MAXSIZE;
    }

    /**
     * prints the current Progress of the Progressbar
     * @return the current Progressbar
     */
    private String printProgress(boolean added) {
        StringBuilder progressbar = new StringBuilder();
        progressbar.append("█".repeat(stepsize*currentprogress));
        if (added) progressbar.append("█");
        while(progressbar.length() < actualMaxsize) progressbar.append(" ");
        return ("Progress: ["+progressbar+"]\r");
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
