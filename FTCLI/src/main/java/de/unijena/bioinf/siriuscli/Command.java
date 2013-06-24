package de.unijena.bioinf.siriuscli;

public interface Command {

    public String getDescription();

    public String getName();

    public void run(String[] args);

    public String getVersion();

}
