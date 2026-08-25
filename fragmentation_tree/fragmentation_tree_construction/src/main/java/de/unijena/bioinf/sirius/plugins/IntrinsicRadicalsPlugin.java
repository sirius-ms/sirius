package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;

import java.util.*;

public class IntrinsicRadicalsPlugin extends SiriusPlugin {

    private static HashSet<MolecularFormula> INTRINSIC_RADICALS = new HashSet<>(Arrays.stream(new String[]{"C2NS",
 "C2NO2",
         "C4NS",
         "CNS",
         "CF3",
         "C3F3",
         "CFS",
         "C3NS",
         "C2F2N",
         "C4F2N",
         "PS",
         "S2",
         "C3OS",
         "C3FS",
         "C4N",
         "C3FO",
         "C2NS2",
         "C4NO2",
         "C3NO",
         "C3S2",
         "CS2",
         "C2S2",
         "C4F2",
         "C3O2",
         "PS2",
         "C4O2",
         "C2NO",
         "CNS2",
         "C2OS",
         "C4NO", "C2F4", "C3F6","C4F8", "C2F3", "C3F7","C4F9"}).map(MolecularFormula::parseOrThrow).toList());

    private static class IntrinsicRadicalScorer implements DecompositionScorer<Object> {

        @Override
        public Object prepare(ProcessedInput input) {
            return null;
        }

        @Override
        public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Object precomputed) {
            return INTRINSIC_RADICALS.contains(formula) ? 3 : 0;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }
    }

    private final MassMap<MolecularFormula> positiveMap = new MassMap<>(700d), negativeMap = new MassMap<>(700d);

    public IntrinsicRadicalsPlugin() {
        final Charge pos = new Charge(1), neg = new Charge(-1);
        for (MolecularFormula formula : INTRINSIC_RADICALS) {
            positiveMap.put(pos.getMass() + formula.getMass(), formula);
            negativeMap.put(neg.getMass() + formula.getMass(), formula);
        }
    }

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addFragmentScorer(new IntrinsicRadicalScorer());
    }

    @Override
    public void addPossibleIonModesToGraph(ProcessedInput input, Ionization candidate, Set<Ionization> ionModes) {
        super.addPossibleIonModesToGraph(input, candidate, ionModes);
        ionModes.add(new Charge(candidate.getCharge()));
    }

    @Override
    protected DecompositionList transformDecompositionList(ProcessedInput input, ProcessedPeak peak, DecompositionList list) {
        list = super.transformDecompositionList(input, peak, list);
        final MassMap<MolecularFormula> map;
        final int polarity;
        if (input.getExperimentInformation().getPrecursorIonType().getCharge()>0) {
            map = positiveMap;
            polarity=1;
        } else {
            map = negativeMap;
            polarity=-1;
        }
        final DecompositionList decomps = list;
        List<MolecularFormula> molecularFormulas = map.retrieveAll(peak.getMass(), 1e-3).stream().filter(f->decomps.find(f)==null).toList();
        if (molecularFormulas.size()>0) {
            final ArrayList<Decomposition> newList = new ArrayList<>(decomps.getDecompositions());
            final Charge intrinsic = new Charge(polarity);
            newList.addAll(molecularFormulas.stream().map(f->new Decomposition(f, intrinsic, 0d)).toList());
            return new DecompositionList(newList);
        } else return list;
    }
}
