package de.unijena.bioinf.ms.frontend;

public interface InstanceProcessor<T> {

    public boolean setup();

    public boolean validate();

    public void output(T result);

    //public JJob makeJob();

}
