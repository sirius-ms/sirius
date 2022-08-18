package de.unijena.bioinf.ms.frontend.utils.Progressbar;

public interface ProgressVisualizer {
    void visualizeProgress();
    void start();
    void stop();
    ProgressbarCalculator getCalculator();
}
