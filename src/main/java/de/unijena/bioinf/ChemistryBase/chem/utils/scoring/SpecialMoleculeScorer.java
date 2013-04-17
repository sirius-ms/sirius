package de.unijena.bioinf.ChemistryBase.chem.utils.scoring;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.math.DensityFunction;
import de.unijena.bioinf.ChemistryBase.math.PartialParetoDistribution;


public class SpecialMoleculeScorer implements MolecularFormulaScorer {


    private final static PartialParetoDistribution oxygenToHeteroKegg = new PartialParetoDistribution(0, 0.75, 5);
    private final static PartialParetoDistribution rdbeKegg = new PartialParetoDistribution(0, 2, 2);

    private final DensityFunction oxygenToHeteroDistribution;
    private final DensityFunction rdbeDistribution;

    public SpecialMoleculeScorer() {
        this(oxygenToHeteroKegg, rdbeKegg);
    }

    public SpecialMoleculeScorer(DensityFunction oxygenToHeteroDistribution, DensityFunction rdbeDistribution) {
        this.oxygenToHeteroDistribution = oxygenToHeteroDistribution;
        this.rdbeDistribution = rdbeDistribution;
    }

    public DensityFunction getRdbeDistribution() {
        return rdbeDistribution;
    }

    public DensityFunction getOxygenToHeteroDistribution() {
        return oxygenToHeteroDistribution;
    }

    @Override
    public double score(MolecularFormula formula) {
        return Math.log(oxygenToHeteroDistribution.getDensity(formula.hetero2OxygenRatio())) + Math.log(rdbeDistribution.getDensity(formula.rdbe()));
    }

    public double score(double hetero2oxygen, double rdbe) {
        return Math.log(oxygenToHeteroDistribution.getDensity(hetero2oxygen)) + Math.log(rdbe);
    }
}
