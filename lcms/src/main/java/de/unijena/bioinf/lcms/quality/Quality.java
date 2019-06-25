package de.unijena.bioinf.lcms.quality;

public enum Quality implements Comparable<Quality>{

    UNUSABLE,   // recommended to throw away
    BAD,        // don't trust
    DECENT,     // decent quality
    GOOD;       // best quality


    public boolean betterThan(Quality q) {
        return ordinal() > q.ordinal();
    }

    public boolean notBetterThan(Quality q) {
        return ordinal() <= q.ordinal();
    }

}
