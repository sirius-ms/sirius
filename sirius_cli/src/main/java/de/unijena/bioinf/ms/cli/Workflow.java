package de.unijena.bioinf.ms.cli;

import java.util.Iterator;

public interface Workflow {

    public boolean setup();

    public boolean validate();

    public void compute(Iterator<Instance> allInstances);

}
