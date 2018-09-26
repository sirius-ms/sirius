package de.unijena.bioinf.ChemistryBase.ms.ft.model;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

public class Decomposition extends Scored<MolecularFormula> {

    protected final Ionization ion;

    public Decomposition(MolecularFormula candidate, Ionization ion, double score) {
        super(candidate, score);
        this.ion = ion;
    }

    public Ionization getIon() {
        return ion;
    }

    public static TCustomHashSet<Decomposition> newDecompositionSet() {
        return new TCustomHashSet<>(new HashWithoutScore());
    }

    protected static class HashWithoutScore implements HashingStrategy<Decomposition> {

        @Override
        public int computeHashCode(Decomposition object) {
            return object.getCandidate().hashCode() ^ 17*object.ion.hashCode();
        }

        @Override
        public boolean equals(Decomposition o1, Decomposition o2) {
            return o1.getCandidate().equals(o2.getCandidate()) && o1.ion.equals(o2.ion);
        }
    }
}
