package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.LossValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.AdductSwitchLossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeScorer;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Set;

public final class AdductSwitchPlugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addLossScorer(new AdductSwitchLossScorer(FragmentationPatternAnalysis.getByClassName(LossSizeScorer.class, initializer.getAnalysis().getPeakPairScorers())));
    }

    @Override
    protected LossValidator filterLossesInGraph(ProcessedInput input, Decomposition root) {
        final IonMode sodium = (IonMode)PrecursorIonType.getPrecursorIonType("[M+Na]+").getIonization();
        if (!root.getIon().equals(sodium)) {
            return null;
        } else {
            return new AllowAdductSwitch();
        }
    }

    @Override
    public void addPossibleIonModesToGraph(ProcessedInput input, Ionization candidate, Set<Ionization> ionModes) {
        Ionization sodium = PrecursorIonType.getPrecursorIonType("[M+Na]+").getIonization();
        if (candidate.equals(sodium)) {
            ionModes.add(PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization());
        }
    }

    private static class AllowAdductSwitch implements LossValidator{
        private Ionization sodium, hplus;
        public AllowAdductSwitch() {
            sodium = PrecursorIonType.getPrecursorIonType("[M+Na]+").getIonization();
            hplus=PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization();

        }

        @Override
        public boolean isForbidden(ProcessedInput input, FGraph graph, Fragment a, Fragment b) {
            if (a.getIonization().equals(hplus) && b.getIonization().equals(sodium)) return true;
            return false;
        }
    }
}
