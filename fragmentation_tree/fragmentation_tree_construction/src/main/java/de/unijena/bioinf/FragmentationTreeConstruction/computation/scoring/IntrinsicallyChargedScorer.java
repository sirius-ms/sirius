
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

/**
 * If the RDBE value of a molecule is a non-integer, the molecule have to be (instrinically) charged. This
 * is, for ESI, very rarely and should be penalized, as all hypothesis with [M+H]+ should have higher probability.
 */
public class IntrinsicallyChargedScorer implements DecompositionScorer {

    private double penalty;

    public IntrinsicallyChargedScorer() {
        this(Math.log(0.001));
    }

    public IntrinsicallyChargedScorer(double penalty) {
        this.penalty = penalty;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
        return formula.maybeCharged() ? penalty : 0d;
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
