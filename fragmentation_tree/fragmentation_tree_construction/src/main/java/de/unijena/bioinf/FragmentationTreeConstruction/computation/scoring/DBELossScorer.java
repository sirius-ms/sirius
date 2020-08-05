
package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;

@Called("Loss RDBE")
public class DBELossScorer implements LossScorer {

    private double score;

    public DBELossScorer() {
        this(Math.log(1d / 3d));
    }

    public DBELossScorer(double score) {
        this.score = score;
    }

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        final int rdbe = loss.getFormula().doubledRDBE();
        if (rdbe < 0) return Math.max(Math.log(0.05), Math.abs(rdbe) * score);
        else return 0;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        score = document.getDoubleFromDictionary(dictionary, "score");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "score", score);
    }
}
