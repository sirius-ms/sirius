package de.unijena.bioinf.ms.cli;

public interface InstanceProcessor<T> {

    public boolean setup();

    public boolean validate();

    public void output(T result);

}
