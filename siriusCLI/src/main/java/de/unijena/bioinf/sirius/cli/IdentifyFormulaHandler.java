package de.unijena.bioinf.sirius.cli;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.MultipleTreeComputation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeIterator;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.TreeScoring;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;

import java.io.IOException;
import java.util.*;

public class IdentifyFormulaHandler {

    public static IdentifyFormulaHandler create(Options options) throws IOException {
        final Profile profile = new Profile(options.getProfile());
        profile.fragmentationPatternAnalysis.setDefaultProfile(MutableMeasurementProfile.merge(profile.fragmentationPatternAnalysis.getDefaultProfile(), ProfileOptions.Interpret.getMeasurementProfile(options)));
        return new IdentifyFormulaHandler(profile.fragmentationPatternAnalysis, profile.isotopePatternAnalysis, options.getNumberOfTrees());
    }

    private FragmentationPatternAnalysis ftAnalyzer;
    private IsotopePatternAnalysis isoAnalyzer;
    private int computeOptNTrees;
    private ProgessHandler progress;

    public IdentifyFormulaHandler(FragmentationPatternAnalysis fragmentationPatternAnalysis, IsotopePatternAnalysis isotopePatternAnalysis, int computeOptNTrees) {
        this.ftAnalyzer = fragmentationPatternAnalysis;
        this.isoAnalyzer = isotopePatternAnalysis;
        this.computeOptNTrees = computeOptNTrees;
        this.progress = ProgessHandler.Noop;
    }

    public IdentificationResult identify(Instance experiment) {
        // first check if MS data is present
        final List<IsotopePattern> candidates = isoAnalyzer.deisotope(experiment.experiment, experiment.experiment.getIonMass(), false);
        int maxNumberOfFormulas = 0;
        final HashMap<MolecularFormula, Double> isoFormulas = new HashMap<MolecularFormula, Double>();
        if (candidates.size() > 0) {
            final IsotopePattern pattern = candidates.get(0);
            filterCandidateList(pattern, isoFormulas);
        }

        final ProcessedInput pinput = ftAnalyzer.preprocessing(experiment.experiment);
        MultipleTreeComputation trees = ftAnalyzer.computeTrees(pinput);

        if (isoFormulas.size() > 0) {
            trees = trees.onlyWith(isoFormulas.keySet());
            maxNumberOfFormulas = isoFormulas.size();
        } else {
            maxNumberOfFormulas = pinput.getPeakAnnotationOrThrow(DecompositionList.class).get(pinput.getParentPeak()).getDecompositions().size();
        }

        // compute optimal n trees
        trees = trees.computeMaximal(computeOptNTrees);

        final TreeSet<FTree> treeSet = new TreeSet<FTree>(new Comparator<FTree>() {
            @Override
            public int compare(FTree o1, FTree o2) {
                return Double.compare(o1.getAnnotationOrThrow(TreeScoring.class).getOverallScore(), o2.getAnnotationOrThrow(TreeScoring.class).getOverallScore());
            }
        });

        final TreeIterator iter = trees.iterator(true);
        progress.init(maxNumberOfFormulas);
        while (iter.hasNext()) {
            final FTree tree = iter.next();
            if (tree != null) {
                treeSet.add(tree);
                if (treeSet.size() > computeOptNTrees) treeSet.pollFirst();
            }
            progress.increase(1);
        }
        progress.finished();

        final ArrayList<FTree> list = new ArrayList<FTree>(computeOptNTrees);
        final Iterator<FTree> setIter = treeSet.descendingSet().iterator();
        while (setIter.hasNext()) {
            list.add(setIter.next());
            if (list.size() >= computeOptNTrees) break;
        }

        final IdentificationResult result = new IdentificationResult(list);
        return result;
    }

    public ProgessHandler getProgress() {
        return progress;
    }

    public void setProgress(ProgessHandler progress) {
        this.progress = progress;
    }

    private void filterCandidateList(IsotopePattern candidate, HashMap<MolecularFormula, Double> formulas) {
        if (candidate.getCandidates().size()==0) return;
        if (candidate.getBestScore() <= 0) return;
        final double optscore = candidate.getBestScore();
        final ArrayList<ScoredMolecularFormula> xs = new ArrayList<ScoredMolecularFormula>(candidate.getCandidates());
        Collections.sort(xs, Collections.reverseOrder());
        int n = 1;
        for (; n < xs.size(); ++n) {
            final double score = xs.get(n).getScore();
            final double prev = xs.get(n-1).getScore();
            if (score <= 0 || score/optscore < 0.666 || score/prev < 0.5) break;
        }
        for (int i=0; i < n; ++i) formulas.put(xs.get(i).getFormula(), xs.get(i).getScore());
    }

}
