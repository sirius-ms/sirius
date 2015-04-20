package de.unijena.bioinf.sirius;

public interface Progress {

    public void init(double maxProgress);

    public void update(double currentProgress, double maxProgress, String value);

    public void finished();

    public void info(String message);

}
