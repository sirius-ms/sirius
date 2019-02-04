package de.unijena.bioinf.sirius.plugins;


import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.BitSet;

/**
 * Gather statistics about the number of explained peaks
 *
 * TODO: currently, isotope peaks are not counted...
 */
public class TreeStatisticPlugin extends SiriusPlugin {


    @Override
    public void initializePlugin(PluginInitializer initializer) {

    }


    /**
     * is called before reducing the graph
     * */
    @Override
    protected void afterGraphScoring(ProcessedInput input, FGraph graph) {
        // count how many colors are contained in the graph
        final BitSet color = new BitSet(input.getMergedPeaks().size());
        for (Fragment f : graph) {
            color.set(f.getColor());
        }

        double totalIntensityOfExplainablePeaks=0d, totalIntensityOfPeaks=0d;
        for (int k=0; k < input.getMergedPeaks().size(); ++k) {
            if (color.get(k)) {
                totalIntensityOfExplainablePeaks += input.getMergedPeaks().get(k).getRelativeIntensity();
            }
            totalIntensityOfPeaks += input.getMergedPeaks().get(k).getRelativeIntensity();
        }

        graph.addAnnotation(RememberNumberOfAnnotatablePeaks.class, new RememberNumberOfAnnotatablePeaks(totalIntensityOfExplainablePeaks, totalIntensityOfPeaks));
    }

    @Override
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
        tree.setAnnotation(TreeStatistics.class, makeTreeStatistics(input, graph, tree));
    }

    private TreeStatistics makeTreeStatistics(ProcessedInput input, FGraph graph, FTree tree) {
        final RememberNumberOfAnnotatablePeaks x = graph.getAnnotation(RememberNumberOfAnnotatablePeaks.class);
        double treeIntensity = 0d;
        final FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Fragment f : tree) {
            treeIntensity += ano.get(f).getRelativeIntensity();
        }

        return new TreeStatistics(treeIntensity, treeIntensity / x.totalIntensityOfExplainablePeaks, treeIntensity / x.totalIntensityOfPeaks);
    }

    private static class RememberNumberOfAnnotatablePeaks implements DataAnnotation  {
        private final double totalIntensityOfExplainablePeaks, totalIntensityOfPeaks;

        public RememberNumberOfAnnotatablePeaks(double totalIntensityOfExplainablePeaks, double totalIntensityOfPeaks) {
            this.totalIntensityOfExplainablePeaks = totalIntensityOfExplainablePeaks;
            this.totalIntensityOfPeaks = totalIntensityOfPeaks;
        }
    }


}
