package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;

public class Hetero2CarbonScorer implements MolecularFormulaScorer {

    private final static NormalDistribution keggDistribution = new NormalDistribution(0.5886335, Math.sqrt(0.5550574));

    public static NormalDistribution getRDBEDistributionFromKEGG() {
        return keggDistribution;
    }

    private RealDistribution distribution;

    public Hetero2CarbonScorer(RealDistribution distribution) {
        this.distribution = distribution;
    }

    public Hetero2CarbonScorer() {
        this(keggDistribution);
    }

    @Override
    public double score(MolecularFormula formula) {
        return distribution.getDensity(formula.rdbe());
    }

}
