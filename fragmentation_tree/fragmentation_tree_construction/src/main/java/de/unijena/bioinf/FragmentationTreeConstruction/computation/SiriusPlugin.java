package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakScorer;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

import java.util.Set;

/**
 * The idea of plugins is to organize code by its function. Currently, many specific features like isotope pattern
 * consists of several functions that have to be called at different code locations. By bundling them into a
 * single class, it should be easier to understand the effect of each plugin. The disadvantage is, that the code is not
 * longer located where it is called, making the understanding of the whole workflow more complicated. Furthermore,
 * there is no system for cross-plugin dependencies. Let's hope that we never need them!
 */
public abstract class SiriusPlugin {

    /*
    methods to override
     */

    /**
     * - is called the first time the plugin is loaded
     * - can add new scorers to the SIRIUS workflow
     * - currently, scorers are not implemented as hooks, as we have to be able to inspect them
     * - however, automatic inspection is horrible. We might change this system in future
     */
    public abstract void initializePlugin(PluginInitializer initializer);

    public static class PluginInitializer {

        protected final FragmentationPatternAnalysis analysis;

        PluginInitializer(FragmentationPatternAnalysis analysis) {
            this.analysis = analysis;
        }

        public <T> void addLossScorer(LossScorer<T> scorer) {
            analysis.getLossScorers().add(scorer);
        }

        public <T> void addPeakScorer(PeakScorer scorer) {
            analysis.getFragmentPeakScorers().add(scorer);
        }

        public <T> void addFragmentScorer(DecompositionScorer<T> scorer) {
            analysis.getDecompositionScorers().add(scorer);
        }

        public <T> void addRootScorer(DecompositionScorer<T> scorer) {
            analysis.getRootScorers().add(scorer);
        }


    }

    /*
     * Hooks
     * These methods allow to hook into the fragmentation tree analysis and change the behaviour of the tree computation.
     * It will not be possible to think of all possible special behaviour patterns. I will cover only the things which
     * are already in use. Every new pattern can be integrated afterwards. Hooks should have an empty default implementation.
     */

    protected void afterPreprocessing(ProcessedInput input) {

    }
    protected void beforeDecomposing(ProcessedInput input) {

    }

    protected Set<IonMode> transformPossibleIonModesForParentPeak(ProcessedInput input, Set<IonMode> ionModes) {
        return ionModes;
    }

    protected Set<IonMode> transformPossibleIonModesForFragmentPeaks(ProcessedInput input, Set<IonMode> ionModes) {
        return ionModes;
    }

    protected DecompositionList transformDecompositionList(ProcessedInput input, ProcessedPeak peak, DecompositionList list) {
        return list;
    }

    protected void beforePeakScoring(ProcessedInput input) {

    }

    protected void beforeGraphBuilding(ProcessedInput input) {

    }

    /**
     * is called after building the graph. Is still allowed to add new nodes and egdes to the graph
     */
    protected void afterGraphBuilding(ProcessedInput input, FGraph graph) {

    }

    protected void transferAnotationsFromInputToGraph(ProcessedInput input, FGraph graph) {

    }

    protected void beforeGraphScoring(ProcessedInput input, FGraph graph) {
    }

    protected void afterGraphScoring(ProcessedInput input, FGraph graph) {
    }

    protected void afterTreeComputation(ProcessedInput input, FGraph graph, FTree tree) {
    }

    protected void transferAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree) {
    }

    /**
     * this method is ONLY called for optimal trees that are reported to the user
     */
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
    }








    }
