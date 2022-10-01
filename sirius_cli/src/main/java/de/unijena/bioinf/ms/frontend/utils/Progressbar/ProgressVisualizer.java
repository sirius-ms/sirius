package de.unijena.bioinf.ms.frontend.utils.Progressbar;

public interface ProgressVisualizer {
    void visualizeProgress();
    void start();
    void println(String message);
    void stop();
    ProgressbarCalculator getCalculator();
}
