package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoreName;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationPathway;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

@ScoreName("pure element penalty")
public class PureCarbonNitrogenLossScorer implements LossScorer {

    private final double epsilon;
    private final Element nitrogen;

    public PureCarbonNitrogenLossScorer(double epsilon) {
        this.epsilon = epsilon;
        this.nitrogen = PeriodicTable.getInstance().getByName("N");
    }

    public PureCarbonNitrogenLossScorer() {
        this(Math.log(0.01));
    }

    @Override
    public Object prepare(ProcessedInput input, FragmentationPathway graph) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final MolecularFormula f = loss.getLoss();
        final int c = f.atomCount();
        if (f.numberOfCarbons() == c || f.numberOf(nitrogen) == c) return epsilon;
        else return 0;
    }
}
