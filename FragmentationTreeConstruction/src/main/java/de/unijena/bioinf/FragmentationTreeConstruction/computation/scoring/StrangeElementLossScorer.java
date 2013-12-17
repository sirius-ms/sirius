package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class StrangeElementLossScorer implements LossScorer {

    private HashSet<MolecularFormula> knownLosses;
    private double score;

    public StrangeElementLossScorer() {
        this.knownLosses = new HashSet<MolecularFormula>();
        this.score = 0d;
    }

    public StrangeElementLossScorer(Iterable<MolecularFormula> knownLosses, double score) {
        this.knownLosses = new HashSet<MolecularFormula>(knownLosses instanceof Collection ?
                (int)(((Collection<MolecularFormula>)knownLosses).size()*1.5) : 150);
        for (MolecularFormula f : knownLosses) this.knownLosses.add(f);
        this.score = score;
    }

    public StrangeElementLossScorer(CommonLossEdgeScorer scorer) {
        this(scorer, Math.log(1.5));
    }

    public StrangeElementLossScorer(CommonLossEdgeScorer scorer, double score) {
        final TObjectDoubleHashMap<MolecularFormula> map = scorer.getRecombinatedList();
        this.knownLosses = new HashSet<MolecularFormula>(150);
        for (MolecularFormula f : map.keySet()) {
            if (!f.isCHNO()) knownLosses.add(f);
        }
        this.score = score;
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        if (knownLosses.contains(loss)) return score;
        else return 0d;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L list = document.getListFromDictionary(dictionary, "losses");
        final int n = document.sizeOfList(list);
        this.knownLosses = new HashSet<MolecularFormula>((int)(n*1.5));
        for (int i=0; i < n; ++i) knownLosses.add(MolecularFormula.parse(document.getStringFromList(list, i)));
        this.score = document.getDoubleFromDictionary(dictionary, "score");

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final L losses = document.newList();
        for (MolecularFormula f : knownLosses) document.addToList(losses, f.toString());
        document.addListToDictionary(dictionary, "losses", losses);
        document.addToDictionary(dictionary, "score", score);
    }
}
