package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;

public class FinestructureGenerator {

    private final IsotopicDistribution distribution;
    private final Normalization mode;
    private final Ionization ion;

    public FinestructureGenerator(IsotopicDistribution dist, Ionization ion, Normalization mode) {
        this.distribution = dist;
        this.ion = ion;
        this.mode = mode;
        if (ion == null || mode == null || distribution == null)
            throw new NullPointerException("Expect non null parameters");
    }

    public class Iterator {

        private final MolecularFormula formula;

        public Iterator(MolecularFormula formula) {
            this.formula = formula;
        }
    }

}
