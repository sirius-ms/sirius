package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.math.RealDistribution;

/**
 * Prior based on ring double bond equation (RDBE) value of the molecule
 */
public class RDBENormalScorer implements MolecularFormulaScorer{

    private final static NormalDistribution keggDistribution = new NormalDistribution(6.151312, Math.sqrt(4.541604));


    public static NormalDistribution getRDBEDistributionFromKEGG() {
        return keggDistribution;
    }

    private DensityFunction distribution;

    public RDBENormalScorer(RealDistribution distribution) {
        this.distribution = distribution;
    }

    public RDBENormalScorer() {
        this(keggDistribution);
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(distribution.getDensity(formula.rdbe()));
    }
}
