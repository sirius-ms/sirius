package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;

/**
 * @see Hetero2CarbonScorer
 * There are many compounds which consists of carbon but also of many oxygen and, therefore, have worse h2c scores.
 * This scorer ignores the number of oxygen. Surprisingly, this seems to result in much better scores.
 */
public class ImprovedHetero2CarbonScorer implements MolecularFormulaScorer{

    private final static PartialParetoDistribution keggDistribution = new PartialParetoDistribution(0, 0.4d, 3.14534);

    private DensityFunction distribution;

    public ImprovedHetero2CarbonScorer() {
        this(keggDistribution);
    }

    public ImprovedHetero2CarbonScorer(@Parameter("distribution") DensityFunction distribution) {
        this.distribution = distribution;
    }

    public void setDistribution(DensityFunction distribution) {
        this.distribution = distribution;
    }

    public DensityFunction getDistribution() {
        return distribution;
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(distribution.getDensity(formula.heteroWithoutOxygenToCarbonRatio()));
    }
}
