package de.unijena.bioinf.FTAnalysis;

public enum LearnMethod {

    SKIP, // skip analysis step
    REPLACE, // replace previous values by new values
    ADD, // add new values to previous values
    MERGE; // merge previous and new values

}
