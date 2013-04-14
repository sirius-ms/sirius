package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;

public class Hydrogen2CarbonScorer implements MolecularFormulaScorer{

    private final static NormalDistribution keggDistribution = new NormalDistribution(1.435877, Math.sqrt(0.4960778));

    public static NormalDistribution getRDBEDistributionFromKEGG() {
        return keggDistribution;
    }

    private RealDistribution distribution;

    public Hydrogen2CarbonScorer(RealDistribution distribution) {
        this.distribution = distribution;
    }

    public Hydrogen2CarbonScorer() {
        this(keggDistribution);
    }

    @Override
    public double score(MolecularFormula formula) {
        return distribution.getDensity(formula.rdbe());
    }

}
