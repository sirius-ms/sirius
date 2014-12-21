package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

/**
 * Na+ and K+ might be typical adducts in MS/MS. If a compound is ionized with such an adduct, it shouldn't loose it
 * during fragmentation. I exclude Cl as this element might also occur in organic compounds.
 */
public class AdductFragmentScorer implements LossScorer<Element[]> {

    protected double penalty = Math.log(0.05);

    @Override
    public Element[] prepare(ProcessedInput input) {
        final PeriodicTable pt = PeriodicTable.getInstance();
        if (input.getExperimentInformation().getIonization().getCharge() < 0) {
            return new Element[]{};
        } else {
            return new Element[]{pt.getByName("Na"), pt.getByName("K")};
        }
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Element[] precomputed) {
        for (Element e : precomputed)
            if (loss.getFormula().numberOf(e) > 0) {
                return penalty;
            }
        return 0d;
    }


    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.penalty = document.getDoubleFromDictionary(dictionary, "score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "score", penalty);
    }
}
