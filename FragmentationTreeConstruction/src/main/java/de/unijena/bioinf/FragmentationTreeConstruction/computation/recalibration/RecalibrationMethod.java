package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FragmentationTree;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import org.apache.commons.math3.analysis.UnivariateFunction;

public interface RecalibrationMethod extends Parameterized {

    public interface Recalibration {
        public double getScoreBonus();
        public FragmentationTree getCorrectedTree(FragmentationPatternAnalysis analyzer);
        public boolean shouldRecomputeTree();
        public UnivariateFunction recalibrationFunction();
    }

    /**
     * recalibrate fragmentation tree
     * @return score bonus through recalibration
     */
    public Recalibration recalibrate(FragmentationTree tree,MassDeviationVertexScorer scorer);

}
