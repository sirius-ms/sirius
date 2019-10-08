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

        final double baseIntensity = input.getMergedPeaks().stream().mapToDouble(x->x.getRelativeIntensity()).max().orElse(1d);

        double totalIntensityOfExplainablePeaks=0d, totalIntensityOfPeaks=0d;
        for (int k=0; k < input.getMergedPeaks().size(); ++k) {
            if (color.get(k)) {
                totalIntensityOfExplainablePeaks += input.getMergedPeaks().get(k).getRelativeIntensity()/baseIntensity;
            }
            totalIntensityOfPeaks += input.getMergedPeaks().get(k).getRelativeIntensity()/baseIntensity;
        }

        graph.addAnnotation(RememberNumberOfAnnotatablePeaks.class, new RememberNumberOfAnnotatablePeaks(totalIntensityOfExplainablePeaks, totalIntensityOfPeaks,baseIntensity));
    }

    @Override
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
        tree.setAnnotation(TreeStatistics.class, makeTreeStatistics(input, graph, tree));
    }

    private TreeStatistics makeTreeStatistics(ProcessedInput input, FGraph graph, FTree tree) {
        final RememberNumberOfAnnotatablePeaks x = graph.getAnnotationOrThrow(RememberNumberOfAnnotatablePeaks.class);
        double treeIntensity = 0d;
        final double baseIntensity = x.baseIntensity;
        final FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Fragment f : tree) {
            treeIntensity += ano.get(f).getRelativeIntensity()/baseIntensity;
        }

        return new TreeStatistics(treeIntensity, treeIntensity / x.totalIntensityOfExplainablePeaks, treeIntensity / x.totalIntensityOfPeaks);
    }

    private static class RememberNumberOfAnnotatablePeaks implements DataAnnotation  {
        private final double totalIntensityOfExplainablePeaks, totalIntensityOfPeaks, baseIntensity;

        public RememberNumberOfAnnotatablePeaks(double totalIntensityOfExplainablePeaks, double totalIntensityOfPeaks, double baseIntensity) {
            this.totalIntensityOfExplainablePeaks = totalIntensityOfExplainablePeaks;
            this.totalIntensityOfPeaks = totalIntensityOfPeaks;
            this.baseIntensity = baseIntensity;
        }
    }


}
