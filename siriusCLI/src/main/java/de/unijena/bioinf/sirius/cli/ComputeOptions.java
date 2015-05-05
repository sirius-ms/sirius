package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;

public interface ComputeOptions extends TreeOptions {

    @Option(shortName = "f", longName = "formula", description = "the molecular formula of the input compound", defaultToNull = true)
    public String getMolecularFormula();

}
