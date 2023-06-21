package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectDoubleProcedure;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Called("CommonRootLoss")
public class CommonRootLossScorer implements FragmentScorer<MolecularFormula>, Parameterized {

    protected final TObjectDoubleHashMap<MolecularFormula> scoring;
    protected double normalization;

    public CommonRootLossScorer() {
        this.scoring = new TObjectDoubleHashMap<>(100, 0.75f, 0d);
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        scoring.clear();
        final Iterator<Map.Entry<String, G>> iter = document.iteratorOfDictionary(document.getDictionaryFromDictionary(dictionary, "rootLosses"));
        while (iter.hasNext()) {
            final Map.Entry<String,G> entry = iter.next();
            try {
                scoring.put(MolecularFormula.parse(entry.getKey()), document.getDouble(entry.getValue()));
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(getClass()).warn("Cannot parse Formula. Skipping!", e);
            }
            normalization = document.getDoubleFromDictionary(dictionary, "normalization");
        }
    }

    @Override
    public <G, D, L> void exportParameters(final ParameterHelper helper, final DataDocument<G, D, L> document, final D dictionary) {
        final D common = document.newDictionary();
        scoring.forEachEntry(new TObjectDoubleProcedure<MolecularFormula>() {
            @Override
            public boolean execute(MolecularFormula a, double b) {
                document.addToDictionary(common, a.toString(), b);
                return true;
            }
        });
        document.addDictionaryToDictionary(dictionary, "rootLosses", common);
        document.addToDictionary(dictionary, "normalization", normalization);
    }

    @Override
    public MolecularFormula prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        Optional<Fragment> root = FragmentScorer.getDecompositionRootNode(graph);
        if (root.isEmpty()) {
            LoggerFactory.getLogger(CommonRootLossScorer.class).warn("Cannot score root losses for graph with multiple roots. Rootloss score is set to 0 for this instance.");
            return null;
        }
        return root.get().getFormula();
    }

    @Override
    public double score(Fragment graphFragment, ProcessedPeak correspondingPeak, boolean isRoot, MolecularFormula root) {
        if (root==null) return 0d;
        final MolecularFormula difference = root.subtract(graphFragment.getFormula());
        return scoring.get(difference) - normalization;
    }
}
