package de.unijena.bioinf.sirius.cli;

import com.lexicalscope.jewel.cli.Option;

public interface IdentifyOptions {

    @Option(shortName = "n", defaultValue = "0", description = "number of trees to compute. By default keep only correct tree (if molecular formula is given) or optimal one otherwise")
    public int getNumberOfTrees();

}
