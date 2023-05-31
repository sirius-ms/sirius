package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormulaMap;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DBPairedScorer;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultInstanceProvider;
import de.unijena.bioinf.ms.properties.DefaultProperty;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.DecompositionList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class BottomUpSearch extends SiriusPlugin {

    public final static MolecularFormulaMap MOLECULAR_FORMULA_MAP;
    static {
        MolecularFormulaMap map = null;
        try (final ObjectInputStream OIN =  new ObjectInputStream(new GZIPInputStream(BottomUpSearch.class.getResourceAsStream("/bioformulas.bin.gz")))) {
            map = (MolecularFormulaMap) OIN.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        MOLECULAR_FORMULA_MAP = map;
    }

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addFragmentScorer(new DBPairedScorer());
    }

    @Override
    protected void beforeDecomposing(ProcessedInput input) {
        super.beforeDecomposing(input);
        if (input.getAnnotation(BottomUpSearchSettings.class, BottomUpSearchSettings::disabled).enabled) {
            final Object2DoubleOpenHashMap<MolecularFormula> weighting = new Object2DoubleOpenHashMap<>();
            final Deviation dev = input.getAnnotation(MS2MassDeviation.class).map(x->x.allowedMassDeviation).orElse(new Deviation(5));
            for (ProcessedPeak peak :input.getMergedPeaks()) {
                final double mz = peak.getMass();
                final double rootLossMass = input.getExperimentInformation().getIonMass() - mz;
                final MolecularFormula[] losses = MOLECULAR_FORMULA_MAP.searchMass(rootLossMass, dev);
                for (MolecularFormula fragment : MOLECULAR_FORMULA_MAP.searchMass(mz, dev)) {
                    for (MolecularFormula loss : losses) {
                        final MolecularFormula together = fragment.add(loss);
                        weighting.addTo(together, peak.getRelativeIntensity());
                    }
                }
            }
            final HashSet<MolecularFormula> formulas = new HashSet<>();
            weighting.forEach((formula, weight)->{
                if (weight >= 0.05) {
                    formulas.add(formula);
                }
            });
            input.setAnnotation(Whiteset.class, input.getAnnotation(Whiteset.class, Whiteset::empty).add(Whiteset.ofMeasuredFormulas(formulas)));
        }
    }


    public static class BottomUpSearchSettings implements Ms2ExperimentAnnotation {
        public final boolean enabled;

        protected final static BottomUpSearchSettings ENABLED = new BottomUpSearchSettings(true), DISABLED = new BottomUpSearchSettings(false);

        public static BottomUpSearchSettings enabled() {
            return ENABLED;
        }
        public static BottomUpSearchSettings disabled() {
            return DISABLED;
        }

        @DefaultInstanceProvider
        public static BottomUpSearchSettings newInstance(@DefaultProperty boolean enabled) {
            return enabled ? ENABLED : DISABLED;
        }


        public BottomUpSearchSettings(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
