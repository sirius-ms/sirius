package de.unijena.bioinf.quality_assessment;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;

import java.util.List;


/**
 * Assessment of compound quality based on the computed fragmentation trees.
 */
public class TreeQualityEvaluator {
    //todo when more quality assessment methods are introduced a QualityAnnotatorUtils class might be useful.

    protected static final double DEFAULT_MIN_EXPLAINED_INTENSITY = 0.8;
    protected static final int DEFAULT_MIN_EXPLAINED_PEAKS = 5;
    double minExplainedIntensity;
    int minExplainedPeaks;

    public TreeQualityEvaluator(){
        this(DEFAULT_MIN_EXPLAINED_INTENSITY, DEFAULT_MIN_EXPLAINED_PEAKS);
    }

    public TreeQualityEvaluator(double minExplainedIntensity, int minExplainedPeaks) {
        this.minExplainedIntensity = minExplainedIntensity;
        this.minExplainedPeaks = minExplainedPeaks;
    }

    /**
     * tests if the provided fragmentation trees provide good information on the measured compound
     * @param fragmentationTrees fragmentation trees computed for one compound
     * @param minExplainedIntensity minimum amount of intensity at least one tree has to explain
     * @param minExplainedPeaks minimum number of peaks at least one tree has to explain
     * @return
     */
    protected boolean isAllCandidatesPoorlyExplainSpectrum(List<FTree> fragmentationTrees, double minExplainedIntensity, int minExplainedPeaks) {
        if (!atLeastOneTreeExplainsSomeIntensity(fragmentationTrees, minExplainedIntensity) ||
            !atLeastOneTreeExplainsSomePeaks(fragmentationTrees, minExplainedPeaks)) return true; //is poorly explained
        return false;
    }

    private static boolean atLeastOneTreeExplainsSomeIntensity(List<FTree> trees, double minExplainedIntensity){
        for (FTree tree : trees) {
            final double intensity = (new FTreeMetricsHelper(tree)).getExplainedIntensityRatio();
            if (intensity>minExplainedIntensity) return true;
        }
        return false;
    }

    private static boolean atLeastOneTreeExplainsSomePeaks(List<FTree> trees, int minExplainedPeaks){
        for (FTree tree : trees) {
            final double numberOfExplainedPeaks = (new FTreeMetricsHelper(tree)).getNumOfExplainedPeaks();
            if (numberOfExplainedPeaks>=minExplainedPeaks) return true;
        }
        return false;
    }


    //jobs

    public JJob<Boolean> makeIsAllCandidatesPoorlyExplainSpectrumJob(final List<FTree> fragmentationTrees){
        return new BasicMasterJJob<Boolean>(JJob.JobType.CPU) {

            @Override
            protected Boolean compute() throws Exception {
                return isAllCandidatesPoorlyExplainSpectrum(fragmentationTrees, minExplainedIntensity, minExplainedPeaks);
            }
        };
    }
}
