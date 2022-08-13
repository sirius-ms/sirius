package de.unijena.bioinf.ms.frontend.utils.Progressbar;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.security.InvalidParameterException;

/**
 * class for a Progressbar instance.
 * Do not print to the given PrintStream while the Progressbar is not finished
 */
public class ProgressbarDefaultCalculator implements ProgressbarCalculator {
    private Integer currentprogress;
    private final Integer maxprogress;
    private final Integer stepsize;
    private final Integer actualMaxsize;
    private final Integer MAXSIZE = 32;

    /**
     * generated a Progressbar instance. Please use the println function of this Progressbar instance
     * to send additional Messages via the PrintStream
     * @param steps the amount of different Positions in the Progressbar
     */
    public ProgressbarDefaultCalculator(@NotNull Integer steps) {
        this.maxprogress = steps;
        this.currentprogress = 0;
        this.stepsize = calculateStepsize(steps);
        this.actualMaxsize = stepsize*maxprogress;
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

    @Override
    public Integer getProgress() {
        return stepsize*currentprogress;
    }

    public Integer getMaxsize() {
        return actualMaxsize;
    }
}
