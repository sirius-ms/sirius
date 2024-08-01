package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.ImplicitAdduct;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.FragmentScorer;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

public class AdductNeutralizationPlugin extends SiriusPlugin {
    @Called("ImplicitAdductPeakScore")
    protected static class AdductLossScorer implements FragmentScorer<FragmentAnnotation<ImplicitAdduct>> {

        @Override
        public FragmentAnnotation<ImplicitAdduct> prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
            return graph.getFragmentAnnotationOrNull(ImplicitAdduct.class);
        }

        @Override
        public double score(Fragment graphFragment, ProcessedPeak correspondingPeak, boolean isRoot, FragmentAnnotation<ImplicitAdduct> prepared) {
            if (prepared != null) {
                final ImplicitAdduct f = prepared.get(graphFragment);
                if (f!=null) return f.getScore();
            }
            return 0d;
        }
    }

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addFragmentScorer(new AdductLossScorer());
    }
}
