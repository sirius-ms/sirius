package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DBPairedScorer;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
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
import java.util.*;
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

    public static List<Decomposition> generateDecompositions(ProcessedInput input) {
        final ValenceFilter filter = new ValenceFilter();

        final Object2DoubleOpenHashMap<Decomposition> weighting = new Object2DoubleOpenHashMap<>();
        final Deviation dev = input.getAnnotation(MS2MassDeviation.class).map(x->x.allowedMassDeviation).orElse(new Deviation(5));
        final Set<IonMode> ionModes = input.getAnnotationOrThrow(PossibleAdducts.class).getIonModes();
        if (!input.getExperimentInformation().getPrecursorIonType().isIonizationUnknown()) {
            ionModes.clear();
            ionModes.add((IonMode)input.getExperimentInformation().getPrecursorIonType().getIonization());
        }
        for (ProcessedPeak peak :input.getMergedPeaks()) {
            final double rootLossMass = input.getExperimentInformation().getIonMass() - peak.getMass();
            final MolecularFormula[] losses = MOLECULAR_FORMULA_MAP.searchMass(rootLossMass, dev);
            for (Ionization ionMode : ionModes) {
                final double mz = ionMode.subtractFromMass(peak.getMass());
                if (mz < 1) continue;
                for (MolecularFormula fragment : MOLECULAR_FORMULA_MAP.searchMass(mz, dev)) {
                    for (MolecularFormula loss : losses) {
                        final MolecularFormula together = fragment.add(loss);
                        if (dev.inErrorWindow(input.getExperimentInformation().getIonMass(), ionMode.addToMass(together.getMass())) && !together.maybeCharged() && filter.isValid(together, ionMode)) {
                            weighting.addTo(new Decomposition(together, ionMode, 0d), Math.max(peak.getRelativeIntensity(),  1e-3));
                        }
                    }
                }
            }
        }
        final HashSet<Decomposition> formulas = new HashSet<>();
        weighting.forEach((formula, weight)->{
            if (weight >= 0.05) {
                formulas.add(formula);
            }
        });
        return new ArrayList<>(formulas);
    }

    // TODO: refactor
    @Override
    protected void beforeDecomposing(ProcessedInput input) {
        super.beforeDecomposing(input);
        final ValenceFilter filter = new ValenceFilter();
        BottomUpSearchSettings settings = input.getAnnotation(BottomUpSearchSettings.class, BottomUpSearchSettings::disabled);
        if (settings.isEnabledFor(input.getExperimentInformation().getIonMass())) {
            // urks, we have to rewrite this horrible recalibration routine. if we recalibrate a tree, we have to fix its molecular formula.
            // same is when the user specifies a molecular formula in the file
            if (!input.getAnnotation(Whiteset.class, Whiteset::denovo).isStillAllowDeNovo()) return;
            final Set<IonMode> ionModes = input.getAnnotationOrThrow(PossibleAdducts.class).getIonModes();
            if (!input.getExperimentInformation().getPrecursorIonType().isIonizationUnknown()) {
                ionModes.clear();
                ionModes.add((IonMode)input.getExperimentInformation().getPrecursorIonType().getIonization());
            }
            final Object2DoubleOpenHashMap<MolecularFormula> weighting = new Object2DoubleOpenHashMap<>();
            final Deviation dev = input.getAnnotation(MS2MassDeviation.class).map(x->x.allowedMassDeviation).orElse(new Deviation(5));
            for (ProcessedPeak peak :input.getMergedPeaks()) {
                final double rootLossMass = input.getExperimentInformation().getIonMass() - peak.getMass();
                final MolecularFormula[] losses = MOLECULAR_FORMULA_MAP.searchMass(rootLossMass, dev);
                for (Ionization ionMode : ionModes) {
                    final double mz = ionMode.subtractFromMass(peak.getMass());
                    if (mz < 1) continue;
                    for (MolecularFormula fragment : MOLECULAR_FORMULA_MAP.searchMass(mz, dev)) {
                        for (MolecularFormula loss : losses) {
                            final MolecularFormula together = fragment.add(loss);
                            if (dev.inErrorWindow(input.getExperimentInformation().getIonMass(), ionMode.addToMass(together.getMass())) && !together.maybeCharged() && filter.isValid(together, ionMode)) {
                                weighting.addTo(together, Math.max(peak.getRelativeIntensity(),  1e-3));
                            }
                        }
                    }
                }
            }
            final HashSet<MolecularFormula> formulas = new HashSet<>();
            weighting.forEach((formula, weight)->{
                if (weight >= 0.05) {
                    formulas.add(formula);
                }
            });

            input.setAnnotation(Whiteset.class, input.getAnnotation(Whiteset.class, Whiteset::empty).add(Whiteset.ofMeasuredFormulas(formulas)).addDeNovo(settings.stillUseDeNovoFor(input.getExperimentInformation().getIonMass())));
            // override constraints... its a bit strange that we have to do that... I would have said when we have
        }
    }


}
