package de.unijena.bioinf.sirius.cli;

public interface Task {

    public String getName();

    public String getDescription();

    public void setArgs(String[] args);

    public void run();

}
