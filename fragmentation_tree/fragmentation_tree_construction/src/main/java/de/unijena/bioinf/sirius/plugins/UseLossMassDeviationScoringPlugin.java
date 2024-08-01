package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationEdgeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.MassDeviationVertexScorer;

public class UseLossMassDeviationScoringPlugin extends SiriusPlugin {
    @Override
    public void initializePlugin(PluginInitializer initializer) {
        final MassDeviationEdgeScorer edge = new MassDeviationEdgeScorer();
        //edge.deviation = new Deviation(10,0.001);
        final MassDeviationVertexScorer vertex = FragmentationPatternAnalysis.getByClassName(MassDeviationVertexScorer.class, initializer.getAnalysis().getDecompositionScorers());
        if (vertex!=null) {
            edge.setWeight(0.5d);
            vertex.setWeight(0.5d);
        }

        initializer.addLossScorer(edge);
    }
}
