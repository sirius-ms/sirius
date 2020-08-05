
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

public class PureCarbonNitrogenLossScorer implements LossScorer<Element[]> {

    private double penalty;

    public PureCarbonNitrogenLossScorer() {
        this(Math.log(0.1));
    }

    public PureCarbonNitrogenLossScorer(double penalty) {
        this.penalty = penalty;
    }

    @Override
    public Element[] prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        final PeriodicTable T = PeriodicTable.getInstance();
        return new Element[]{T.getByName("Cl"), T.getByName("K"),T.getByName("Na")};
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Element[] halogens) {
        final MolecularFormula f = loss.getFormula();
        final int nitrogen = f.numberOfNitrogens();
        final int carbon = f.numberOfCarbons();
        final int both = nitrogen+carbon;
        final int atomcount = f.atomCount();
        if (both >= atomcount) return penalty;
        else if (both >= atomcount-1) {
            // exclude single Na, Cl and K losses
            for (Element e : halogens) {
                if (f.numberOf(e) > 0) return penalty;
            }
            return 0d;
        } else return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        penalty = document.getDoubleFromDictionary(dictionary, "penalty");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "penalty", penalty);
    }
}
