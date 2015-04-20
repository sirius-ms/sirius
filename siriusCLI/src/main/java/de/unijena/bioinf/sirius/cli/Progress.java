package de.unijena.bioinf.sirius.cli;

/**
 * Created by kaidu on 20.04.2015.
 */
public interface Progress {

    public void init(double maxProgress);
    public void update(double currentProgress, double maxProgress, String value);
    public void finished();
    public void info(String message);

}
