package de.unijena.bioinf.ms.frontend.utils.Progressbar;

public interface ProgressbarCalculator {
    void increaseProgress();
    void decreaseProgress();
    Integer getProgress();
    Integer getMaxsize();
}
