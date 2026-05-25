package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSearchSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DBPairedScorer;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.elementdetection.DetectedFormulaConstraints;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class BottomUpSearch extends SiriusPlugin {

    private final static int RARE_ELEMENT_N_PEAKS = 3,
                             COMMON_N_PEAKS = 1;

    private final static double RARE_ELEMENT_INTENSITY = 0.15,
                                COMMON_INTENSITY = 0.05;


    public final static MolecularFormulaMap MOLECULAR_FORMULA_MAP;
    static {
        MolecularFormulaMap map = null;
        try (final ObjectInputStream OIN =  new ObjectInputStream(new GZIPInputStream(BottomUpSearch.class.getResourceAsStream("/bioformulas.bin.gz")))) {
            map = (MolecularFormulaMap) OIN.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        MOLECULAR_FORMULA_MAP = map;


        List<MolecularFormula> formulas = new ArrayList<>();
        for (MolecularFormula f : map) {
            formulas.add(f);
        }
        formulas.sort(Comparator.comparingDouble(x->x.getMass()));
        try (final PrintStream out = new PrintStream("/home/kaidu/temp/fs.txt")) {
            for (MolecularFormula f : formulas) out.println(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addFragmentScorer(new DBPairedScorer());
    }

    /**
     *
     * @param input
     * @param addToWhiteset adds the decomposed formulas to the whiteset. May apply {@link FormulaConstraints} while adding.
     * @return
     */
    private static List<Decomposition> generateDecompositions(ProcessedInput input, boolean addToWhiteset) {

        FormulaConstraints formulaConstraints = input.getAnnotationOrDefault(FormulaConstraints.class);
        ElementsDetectedAsAbsent elementsDetectedAsAbsent = input.getAnnotation(ElementsDetectedAsAbsent.class).orElse(ElementsDetectedAsAbsent.empty());
        if (addToWhiteset && input.getAnnotationOrThrow(Whiteset.class).isFinalized()) return null;
        final PossibleAdducts possibleAdducts = input.getAnnotationOrThrow(PossibleAdducts.class);
        final ValenceFilter filter = new ValenceFilter(ValenceFilter.MIN_VALENCE_DEFAULT, possibleAdducts.getAdducts());

        final Object2DoubleOpenHashMap<Decomposition> weighting = new Object2DoubleOpenHashMap<>();
        final Object2IntOpenHashMap<Decomposition> counting = new Object2IntOpenHashMap<>();
        final Deviation dev = input.getAnnotation(MS2MassDeviation.class).map(x->x.allowedMassDeviation).orElse(new Deviation(5));
        final Set<IonMode> ionModes = input.getAnnotationOrThrow(PossibleAdducts.class).getIonModes();
        if (!input.getExperimentInformation().getPrecursorIonType().isIonizationUnknown()) { //todo ElementFilter this should be already decided. This is the wrong location to check again. Or has this to do with recalibration?
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
                            counting.addTo(new Decomposition(together, ionMode, 0d), 1);
                        }
                    }
                }
            }
        }
        final HashSet<Decomposition> decompositions = new HashSet<>();
        HashSet<Element> consideredRareElements = new HashSet<>();
        weighting.forEach((formula, weight)->{
            final int count = counting.getOrDefault(formula, 0);
            if (formula.getCandidate().isCHNOPS() || formulaConstraints.isSatisfied(formula.getCandidate(), PrecursorIonType.getPrecursorIonType(formula.getIon()))) {
                if (weight >= COMMON_INTENSITY && count >= COMMON_N_PEAKS) {
                    decompositions.add(formula);
                }
            } else {
                if (weight >= RARE_ELEMENT_INTENSITY && count >= RARE_ELEMENT_N_PEAKS) {
                    decompositions.add(formula);
                    consideredRareElements.addAll(formula.getCandidate().elements());
                }
            }
        });
        FormulaConstraints extendedConstraints = formulaConstraints.getExtendedConstraints(consideredRareElements.toArray(Element[]::new));

        for (Ionization ionMode : ionModes) {
            final double mz = ionMode.subtractFromMass(input.getExperimentInformation().getIonMass());
            Arrays.stream(MOLECULAR_FORMULA_MAP.searchMass(mz, dev)).filter(f->extendedConstraints.isSatisfied(f,PrecursorIonType.getPrecursorIonType(ionMode))).forEach(x -> decompositions.add(new Decomposition(x,ionMode,0d)));
        }

        if (addToWhiteset) {
            Set<MolecularFormula> formulas = decompositions.stream().map(Decomposition::getCandidate).collect(Collectors.toSet());
            Whiteset ws = input.getAnnotationOrThrow(Whiteset.class);

            //apply filters before adding to whiteset
            //case 1: if "applyFormulaConstraintsToBottomUp" apply the same formula contraints as for de novo.
            //case 2: element filter should only apply to denovo, not bottom up
            //      * Here, we still need to apply the FormulaFilters (not the ChemicalAlphabet)
            //      * Additionally, if element detection has been performed (for 2+ MS1 isotope peaks), all elements that were detectable but have not been detected are forbidden and such formulas removed.
            //        This, keeps e.g. 'F' even if 'F' is not in enforced, since 'F' is not detectable.
            FormulaSearchSettings formulaSearchSettings = input.getAnnotation(FormulaSearchSettings.class, FormulaSearchSettings::bottomUpOnly);
            if (formulaSearchSettings.applyFormulaConstraintsToBottomUp) { //case 1
                formulas = Whiteset.filterMeasuredFormulas(formulas, formulaConstraints, possibleAdducts.getAdducts().stream().filter(x->x.isSupportedForFragmentationTreeComputation()).collect(Collectors.toSet()));
            } else { //case 2
                //filter need to be applied because later we cannot differentiate formulas with and without applied filter anyway but need this to select adducts.
                FormulaConstraints formulaConstraintsWithAllElements = new FormulaConstraints(formulaConstraints.getChemicalAlphabet().extend(formulas.stream().flatMap(mf -> mf.elements().stream()).filter(Predicate.not(Objects::isNull)).distinct().toArray(Element[]::new)), formulaConstraints.getFilters());
                formulas = Whiteset.filterMeasuredFormulas(formulas, formulaConstraintsWithAllElements, possibleAdducts.getAdducts().stream().filter(x->x.isSupportedForFragmentationTreeComputation()).collect(Collectors.toSet()));

                final FormulaConstraints fc = formulaConstraints;
                final FormulaSettings settings = input.getAnnotationOrDefault(FormulaSettings.class);
                formulas = formulas.stream().filter(elementsDetectedAsAbsent::isSatisfied).collect(Collectors.toSet());//formulas = filterUndetectedElements(settings, fc, formulas);
            }

            Whiteset bottomUpWs = Whiteset.ofMeasuredFormulas(formulas, BottomUpSearch.class);
            ws = ws.setRequiresBottomUp(false).add(bottomUpWs);
            input.setAnnotation(Whiteset.class, ws);
        }
        return new ArrayList<>(decompositions);
    }

    public static boolean generateDecompositionsAndSaveToWhiteset(ProcessedInput input) {
        return generateDecompositions(input, true) == null ? false : true;
    }

    // TODO: refactor
    @Override
    protected void beforeDecomposing(ProcessedInput input) {
        super.beforeDecomposing(input);

        //run if enabled
        //todo ElementFilter: the following comment relates to the "!input.getExperimentInformation().getPrecursorIonType().isIonizationUnknown()" check -> maybe not necessary anymore with new whiteset workflow?
        // urks, we have to rewrite this horrible recalibration routine. if we recalibrate a tree, we have to fix its molecular formula.
        // same is when the user specifies a molecular formula in the file
        if (!input.getAnnotationOrThrow(Whiteset.class).stillRequiresBottomUpBeAdded()) return; //has already been performed at the isotope pattern analysis stage.

        generateDecompositionsAndSaveToWhiteset(input);

    }


}
