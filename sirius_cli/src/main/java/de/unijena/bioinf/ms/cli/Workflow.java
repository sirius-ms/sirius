package de.unijena.bioinf.ms.cli;

import java.util.Iterator;

public interface Workflow<I> {

    public boolean setup();

    public boolean validate();

    /*
    compute, output, write into projectspace, etc.
     */
    public void compute(Iterator<I> allInstances); //todo maybe with output Iterator<O>

}
