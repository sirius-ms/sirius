package de.unijena.bioinf.IsotopePatternAnalysis.generation;

final class Isotopologue implements Comparable<Isotopologue> {
    protected final short[] amounts;
    protected final double mass;
    protected final double logAbundance;

    Isotopologue(short[] amounts, double mass, double logAbundance) {
        this.amounts = amounts;
        this.mass = mass;
        this.logAbundance = logAbundance;
    }

    @Override
    public int compareTo(Isotopologue o) {
        return Double.compare(logAbundance, o.logAbundance);
    }
}
