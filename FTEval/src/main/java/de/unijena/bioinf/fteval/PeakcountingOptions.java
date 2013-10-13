package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;

public interface PeakcountingOptions extends EvalBasicOptions {

    @Option(shortName = "l", defaultValue = "0", description = "limit comparison to n most intensive peaks")
    public int getLimit();

    @Option(shortName = "p", defaultValue = "0.01", description = "apply baseline")
    public double getThreshold();

    @Option(shortName = "t", defaultValue = "peakcounting", description = "name of the output file")
    public String getTarget();



}
