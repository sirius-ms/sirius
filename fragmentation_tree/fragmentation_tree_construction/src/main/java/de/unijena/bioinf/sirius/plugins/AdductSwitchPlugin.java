package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdductSwitches;
import de.unijena.bioinf.ChemistryBase.ms.ft.FGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.graph.LossValidator;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.AdductSwitchLossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeScorer;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Map;
import java.util.Set;

public final class AdductSwitchPlugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addLossScorer(new AdductSwitchLossScorer(FragmentationPatternAnalysis.getByClassName(LossSizeScorer.class, initializer.getAnalysis().getPeakPairScorers())));
    }

    @Override
    protected LossValidator filterLossesInGraph(ProcessedInput input, Decomposition root) {
        final PossibleAdductSwitches switches = input.getAnnotation(PossibleAdductSwitches.class,PossibleAdductSwitches::disabled);
        final Map<Ionization,Set<Ionization>> transitions = switches.getTransitions();
        Set<Ionization> ionizations = transitions.get(root.getIon());
        if (ionizations ==null) {
            return null;
        } else {
            return new AllowAdductSwitch(transitions);
        }
    }

    @Override
    public void addPossibleIonModesToGraph(ProcessedInput input, Ionization candidate, Set<Ionization> ionModes) {
        final PossibleAdductSwitches switches = input.getAnnotation(PossibleAdductSwitches.class,PossibleAdductSwitches::disabled);
        ionModes.addAll(switches.getPossibleIonizations(candidate));
    }

    private static class AllowAdductSwitch implements LossValidator{
        private Map<Ionization, Set<Ionization>> transitions;
        private final Element N, P;

        public AllowAdductSwitch(Map<Ionization, Set<Ionization>> transitions) {
            this.transitions = transitions;
            final PeriodicTable pt = PeriodicTable.getInstance();
            N = pt.getByName("N");
            P = pt.getByName("P");
        }

        @Override
        public boolean isForbidden(ProcessedInput input, FGraph graph, Fragment a, Fragment b) {
            if (a.getIonization().equals(b.getIonization()))
                return false; // never forbid when ion stays the same
            Set<Ionization> allowed = transitions.get(a.getIonization());
            if (allowed==null) return true;
            if (allowed.contains(b.getIonization())) {
                if (a.getIonization().getCharge()<0) return false; // for negative we do not know the mechanism
                final MolecularFormula difference = a.getFormula().subtract(b.getFormula());
                if ((difference.numberOfOxygens()>0) || (difference.numberOf(N)>0) || difference.numberOf(P)>0) return false;
                return true;
            } else return true;
        }
    }
}
