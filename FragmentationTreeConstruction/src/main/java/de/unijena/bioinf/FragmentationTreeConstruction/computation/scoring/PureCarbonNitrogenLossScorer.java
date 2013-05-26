package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

public class PureCarbonNitrogenLossScorer implements LossScorer {


    @Override
    public Element prepare(ProcessedInput inputh) {
        return PeriodicTable.getInstance().getByName("N");
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object n) {
        final MolecularFormula f = loss.getFormula();
        final int nitrogen = f.numberOf((Element)n);
        final int carbon = f.numberOfCarbons();
        if (nitrogen+carbon >= f.atomCount()) return Math.log(0.1);
        else return 0d;
    }
}
