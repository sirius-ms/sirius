package de.unijena.bioinf.sirius;

import com.lexicalscope.jewel.cli.Option;

public interface IdentifyOptions extends TreeOptions {


    enum ISO {omit, filter, score};

    @Option(shortName = "i", defaultValue = "score", description = "how to handle isotope pattern data. Use 'score' to use them for ranking or 'filter' if you just want to remove candidates with bad isotope pattern. Use 'omit' to ignore isotope pattern.")
    public ISO getIsotopes();

    @Option(shortName = "c", longName = "candidates", description = "number of candidates in the output", defaultValue = "5")
    public int getNumberOfCandidates();




}
