package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
/*
    Scores the hydrogen-to-carbon (hy2c) ratio of a molecule.
    This value is modelled as normal distribution as there are few possible molecules with low hy2c values (which are,
    hopefully, often common losses and fragments) while high hy2c values makes usually no sense.
    The parameters of the distribution are optimized from kegg after filtering outliers.
    The stretched normal distribution is more tolerant against higher values but still filters enough decompositions.

    Remark that there are molecules with high hy2c values (~11) which should be handled special. A simple approach would
    be to score the rdbe, because in kegg all this compounds have a rdbe < 2.
 */
public class Hydrogen2CarbonScorer implements MolecularFormulaScorer{

    private final static NormalDistribution keggDistribution = new NormalDistribution(1.435877, Math.pow(0.4960778, 2));
    private final static NormalDistribution stretchedKeggDistribution = new NormalDistribution(1.435877, Math.pow(0.75, 2));


    public static NormalDistribution getHydrogenToCarbonDistributionFromKEGG() {
        return keggDistribution;
    }
    public static NormalDistribution getStretchedHydrogenToCarbonDistributionFromKEGG() {
        return keggDistribution;
    }

    private DensityFunction distribution;

    public Hydrogen2CarbonScorer(DensityFunction distribution) {
        this.distribution = distribution;
    }

    public Hydrogen2CarbonScorer() {
        this(stretchedKeggDistribution);
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(distribution.getDensity(formula.hydrogen2CarbonRatio()));
    }

    public double score(double hy2c) {
        return Math.log(distribution.getDensity(hy2c));
    }

}
