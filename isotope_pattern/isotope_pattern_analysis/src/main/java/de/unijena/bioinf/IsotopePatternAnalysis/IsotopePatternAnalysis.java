
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.IsotopePatternScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.NormalDistributedIntensityScorer;
import de.unijena.bioinf.MassDecomposer.Chemistry.AddDeNovoDecompositionsToWhiteset;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.sirius.ProcessedInput;
import org.apache.commons.lang3.Range;

import java.util.*;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.addOffset;
import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.normalize;

public class IsotopePatternAnalysis implements Parameterized {

    public static final String ANALYZER_NAME = "IsotopePatternAnalysis";
    private static final boolean USE_ALWAYS_THE_COMPLETE_PATTERN = false;

    private List<IsotopePatternScorer> isotopePatternScorers;
    private double intensityOffset;
    private DecomposerCache decomposer;
    private IsotopicDistribution isotopicDistribution;
    private IsotopePatternGenerator patternGenerator;

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "patternScorers")) {
            final Iterator<G> scorers = document.iteratorOfList(document.getListFromDictionary(dictionary, "patternScorers"));
            while (scorers.hasNext()) {
                getIsotopePatternScorers().add((IsotopePatternScorer) helper.unwrap(document, scorers.next()));
            }
        }
        if (document.hasKeyInDictionary(dictionary, "intensityOffset"))
            setIntensityOffset(document.getDoubleFromDictionary(dictionary, "intensityOffset"));
        if (document.hasKeyInDictionary(dictionary, "isotopes"))
            setIsotopicDistribution((IsotopicDistribution) helper.unwrap(document, document.getFromDictionary(dictionary, "isotopes")));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // export isotope distribution for relevant elements
        final ChemicalAlphabet alphabet = ChemicalAlphabet.getExtendedAlphabet();
        final IsotopicDistribution dist = isotopicDistribution.subset(alphabet.getElements());
        document.addToDictionary(dictionary, "isotopes", helper.wrap(document, dist));
        final L scorers = document.newList();
        for (IsotopePatternScorer scorer : isotopePatternScorers)
            document.addToList(scorers, helper.wrap(document, scorer));
        document.addListToDictionary(dictionary, "patternScorers", scorers);
        document.addToDictionary(dictionary, "intensityOffset", intensityOffset);

    }

    public static <G, D, L> IsotopePatternAnalysis loadFromProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        if (!document.hasKeyInDictionary(dict, ANALYZER_NAME))
            throw new IllegalArgumentException("No field 'IsotopePatternAnalysis' in profile");
        final IsotopePatternAnalysis analyzer = (IsotopePatternAnalysis) helper.unwrap(document,
                document.getFromDictionary(dict, ANALYZER_NAME));
        return analyzer;
    }

    public <G, D, L> void writeToProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        final D fpa = document.newDictionary();
        exportParameters(helper, document, fpa);
        document.addToDictionary(fpa, "$name", ANALYZER_NAME);
        document.addDictionaryToDictionary(dict, ANALYZER_NAME, fpa);
    }

    public IsotopePatternAnalysis() {
        this.isotopePatternScorers = new ArrayList<IsotopePatternScorer>();
        this.decomposer = new DecomposerCache();
        this.isotopicDistribution = PeriodicTable.getInstance().getDistribution();
        this.intensityOffset = 0d;
        this.patternGenerator = new FastIsotopePatternGenerator(isotopicDistribution, Normalization.Max(1d));
    }

    public static IsotopePatternAnalysis defaultAnalyzer() {
        final PeriodicTable T = PeriodicTable.getInstance();
        final IsotopePatternAnalysis analyzer = new IsotopePatternAnalysis();
//        double offset = 1.323d;
        analyzer.intensityOffset = 0d;
        analyzer.isotopePatternScorers.add(new MassDifferenceDeviationScorer());
        analyzer.isotopePatternScorers.add(new NormalDistributedIntensityScorer());
        final FormulaConstraints constr = new FormulaConstraints(new ChemicalAlphabet(T.getAllByName("C", "H",
                "N", "O", "P", "S", "Cl", "Na")), null);
        constr.setUpperbound(T.getByName("Cl"), 1);
        constr.setUpperbound(T.getByName("Na"), 1);
        constr.setUpperbound(T.getByName("P"), 3);
        constr.setUpperbound(T.getByName("S"), 3);
        constr.setUpperbound(T.getByName("N"), 10);
        constr.setUpperbound(T.getByName("O"), 25);

        return analyzer;
    }

    public double getIntensityOffset() {
        return intensityOffset;
    }

    public void setIntensityOffset(double intensityOffset) {
        this.intensityOffset = intensityOffset;
    }

    public IsotopePatternGenerator getPatternGenerator() {
        return patternGenerator;
    }

    public void setPatternGenerator(IsotopePatternGenerator patternGenerator) {
        this.patternGenerator = patternGenerator;
    }


    public SimpleSpectrum extractPattern(Ms2Experiment experiment, double targetMz) {
        final Spectrum<Peak> s = experiment.getMergedMs1Spectrum();
        final MS1MassDeviation dev = experiment.getAnnotationOrDefault(MS1MassDeviation.class);
        ChemicalAlphabet stdalphabet = experiment.getAnnotationOrDefault(FormulaConstraints.class).getChemicalAlphabet();
        if (s != null) return extractPattern(s, dev, stdalphabet, targetMz);
        else if (experiment.getMs1Spectra() != null && !experiment.getMs1Spectra().isEmpty()) {
            return extractPattern(experiment.getMs1Spectra().get(0), dev, stdalphabet, targetMz);
        } else return null;
    }

    public SimpleSpectrum extractPattern(Spectrum<Peak> ms1Spec, MS1MassDeviation deviation, ChemicalAlphabet stdalphabet, double targetMz) {
        // extract all isotope peaks starting from the given target mz
        final Spectrum<Peak> massOrderedSpectrum = Spectrums.getMassOrderedSpectrum(ms1Spec);
        final int index = Spectrums.mostIntensivePeakWithin(massOrderedSpectrum, targetMz, deviation.allowedMassDeviation);
        if (index < 0) return null;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        spec.addPeak(massOrderedSpectrum.getPeakAt(index));
        // add additional peaks
        for (int k = 1; k <= 5; ++k) {
            final Range<Double> nextMz = PeriodicTable.getInstance().getIsotopicMassWindow(stdalphabet, deviation.allowedMassDeviation, spec.getMzAt(0), k);
            final double a = nextMz.getMinimum();
            final double b = nextMz.getMaximum();
            final double startPoint = a - deviation.massDifferenceDeviation.absoluteFor(a);
            final double endPoint = b + deviation.massDifferenceDeviation.absoluteFor(b);
            final int nextIndex = Spectrums.indexOfFirstPeakWithin(massOrderedSpectrum, startPoint, endPoint);
            if (nextIndex < 0) break;
            double mzBuffer = 0d;
            double intensityBuffer = 0d;
            for (int i = nextIndex; i < massOrderedSpectrum.size(); ++i) {
                final double mz = massOrderedSpectrum.getMzAt(i);
                if (mz > endPoint) break;
                final double intensity = massOrderedSpectrum.getIntensityAt(i);
                mzBuffer += mz * intensity;
                intensityBuffer += intensity;
            }
            mzBuffer /= intensityBuffer;
            spec.addPeak(mzBuffer, intensityBuffer);
        }
        return new SimpleSpectrum(spec);
    }

    /**
     * May update the Whiteset annotation. If de novo decomposition has not been performed yet, adds formulas and sets the whiteset's denovo flag to false.
     * @param input
     * @return
     */
    public boolean computeAndScoreIsotopePattern(ProcessedInput input) {
        final Ms1IsotopePattern pattern = input.getAnnotation(Ms1IsotopePattern.class, Ms1IsotopePattern::none);
        if (!pattern.isEmpty()) {
            final HashMap<MolecularFormula, IsotopePattern> explanations = new HashMap<>();
            final SimpleSpectrum spec = pattern.getSpectrum();
            final MS1MassDeviation massDev = input.getAnnotationOrDefault(MS1MassDeviation.class);
            final PossibleAdducts possibleAdducts = input.getAnnotationOrDefault(PossibleAdducts.class);
            final FormulaConstraints constraints = input.getAnnotationOrDefault(FormulaConstraints.class);
            final double monoMz = spec.getMzAt(0); //should be coherent with FragmentationPatternAnalysis.performDecomposition()

            Whiteset whiteset = input.getAnnotationOrNull(Whiteset.class);
            if (whiteset == null || (!whiteset.isFinalized() && whiteset.stillRequiresDeNovoToBeAdded())) {
                whiteset = AddDeNovoDecompositionsToWhiteset.createNewWhitesetWithDenovoAdded(whiteset, monoMz, massDev.allowedMassDeviation, possibleAdducts, constraints, decomposer);
                input.setAnnotation(Whiteset.class, whiteset);
            }

            for (IonMode ionMode : possibleAdducts.getIonModes()) {
                Set<MolecularFormula> formulas = new HashSet<>();
                //select formulas for the current ionMode
                if (whiteset != null) {
                    final Set<MolecularFormula> resolvedWhiteset = whiteset.resolve(monoMz, massDev.allowedMassDeviation, possibleAdducts.getAdducts(ionMode)).stream().map(d -> d.getCandidate()).collect(Collectors.toSet());//decompositions.stream().filter(decomposition -> decomposition.getIon().equals(ionMode)).map(d -> d.getCandidate()).collect(Collectors.toSet());
                    formulas.addAll(resolvedWhiteset);
                }
                //todo ElementFilter: same task performed multiple times for each ionMode. Remove or move
                PrecursorIonType precursorIonType = input.getExperimentInformation().getPrecursorIonType();
                if (!precursorIonType.hasNeitherAdductNorInsource()) {
                    formulas=formulas.stream().filter(f->precursorIonType.measuredNeutralMoleculeToNeutralMolecule(f).isAllPositiveOrZero()).collect(Collectors.toSet()); //todo ElementFilter: this should be resolved earlier ... maybe the valence/formulaconstraints filter could be used. but on the other hand, this should also be tested earlier...
                }

                for (IsotopePattern pat : scoreFormulas(spec, formulas.stream().toList(), input.getExperimentInformation(), PrecursorIonType.getPrecursorIonType(ionMode))) {
                    explanations.put(pat.getCandidate(), pat);
                }
            }
            input.setAnnotation(ExtractedIsotopePattern.class, new ExtractedIsotopePattern(spec, explanations));
            return true;
        } else return false;
    }

    public List<IsotopePattern> deisotope(Ms2Experiment experiment, List<MolecularFormula> formulas) {
        if (experiment.getMs1Spectra().isEmpty() && experiment.getMergedMs1Spectrum() == null) return new ArrayList<>();

        final SimpleSpectrum pattern = extractPattern(experiment, experiment.getIonMass());
        if (pattern == null) return Collections.emptyList();
        return scoreFormulas(pattern, formulas, experiment);
    }

    public List<IsotopePattern> deisotope(Ms2Experiment experiment) {
        if (experiment.getMs1Spectra().isEmpty() && experiment.getMergedMs1Spectrum() == null) return new ArrayList<>();
        final SimpleSpectrum pattern = extractPattern(experiment, experiment.getIonMass());
        if (pattern == null) return Collections.emptyList();
        final PrecursorIonType ionization = experiment.getPrecursorIonType();
        final FormulaConstraints constraints = experiment.getAnnotationOrDefault(FormulaConstraints.class);
        final MS1MassDeviation deviation = experiment.getAnnotationOrDefault(MS1MassDeviation.class);

        if (ionization.isIonizationUnknown()) {
            // try different ionization types
            final List<IsotopePattern> ionFormulas = new ArrayList<IsotopePattern>();
            final int charge = ionization.getCharge();
            // TODO: update
            List<IonMode> ionModes = new ArrayList<>();
            for (Ionization ion : ionModes) {
                final List<MolecularFormula> formulas =
                        decomposer.getDecomposer(constraints.getChemicalAlphabet()).decomposeToFormulas(pattern.getMzAt(0), ion, deviation.allowedMassDeviation, constraints);
                ionFormulas.addAll(scoreFormulas(pattern, formulas, experiment, PrecursorIonType.getPrecursorIonType(ion)));
            }
            ionFormulas.sort(Comparator.reverseOrder());
            return ionFormulas;
        } else {
            // use given ionization
            final List<MolecularFormula> formulas =
                    decomposer.getDecomposer(constraints.getChemicalAlphabet()).decomposeToFormulas(pattern.getMzAt(0)-ionization.getModificationMass(), ionization.getIonization(), deviation.allowedMassDeviation, constraints);
            return scoreFormulas(pattern, formulas, experiment);
        }
    }

    public List<IsotopePattern> scoreFormulas(SimpleSpectrum extractedSpectrum, List<MolecularFormula> formulas, Ms2Experiment experiment) {
        return scoreFormulas(extractedSpectrum, formulas, experiment, experiment.getPrecursorIonType());
    }

    public List<IsotopePattern> scoreFormulas(SimpleSpectrum extractedSpectrum, List<MolecularFormula> formulas, Ms2Experiment experiment, PrecursorIonType ion) {
        final double cutoff = experiment.getAnnotationOrDefault(IsotopicIntensitySettings.class).minimalIntensityToConsider;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(extractedSpectrum);
        normalize(spec, Normalization.Sum(1d));
        if (intensityOffset != 0d) {
            addOffset(spec, 0d, intensityOffset);
            normalize(spec, Normalization.Sum(1d));
        }

        if (spec.getIntensityAt(0) < cutoff) {
            //intensity of first peak is below cutoff, cannot score
            return new ArrayList<>();
        }
        while (spec.getIntensityAt(spec.size() - 1) < cutoff) spec.removePeakAt(spec.size() - 1);
        normalize(spec, Normalization.Max(1));
        final Spectrum<Peak> measuredSpectrum = new SimpleSpectrum(spec);
        final ArrayList<IsotopePattern> patterns = new ArrayList<>(formulas.size());
        final SimpleSpectrum[] allPatternVariants = new SimpleSpectrum[measuredSpectrum.size()];
        {
            final SimpleMutableSpectrum mut = new SimpleMutableSpectrum(allPatternVariants.length);
            for (int k = 0; k < allPatternVariants.length; ++k) {
                mut.addPeak(measuredSpectrum.getMzAt(k), measuredSpectrum.getIntensityAt(k));
                allPatternVariants[k] = new SimpleSpectrum(mut);
            }
        }
        int k = 0;
        final double[] scoreBuffer = new double[allPatternVariants.length];
        for (MolecularFormula formula : formulas) {
            Arrays.fill(scoreBuffer, 0d);
            final MolecularFormula f = ion.neutralMoleculeToMeasuredNeutralMolecule(formula);
            Spectrum<Peak> measuredOne = measuredSpectrum;
            Spectrum<Peak> theoreticalSpectrum = patternGenerator.simulatePattern(f, ion.getIonization());
            if (theoreticalSpectrum.size() == 0) continue;
            if (theoreticalSpectrum.size() > 10)
                theoreticalSpectrum = Spectrums.getNormalizedSpectrum(Spectrums.subspectrum(theoreticalSpectrum, 0, 10), Normalization.Max(1d));
            if (measuredSpectrum.size() > theoreticalSpectrum.size())
                measuredOne = Spectrums.getNormalizedSpectrum(Spectrums.subspectrum(measuredSpectrum, 0, theoreticalSpectrum.size()), Normalization.Max(1d));
            for (IsotopePatternScorer scorer : isotopePatternScorers) {
                scorer.score(scoreBuffer, measuredOne, theoreticalSpectrum, Normalization.Max(1), experiment);
            }
            int optScoreIndex;
            if (USE_ALWAYS_THE_COMPLETE_PATTERN) {
                optScoreIndex = scoreBuffer.length-1;
            } else {
                optScoreIndex = 0;
                for (int j = 0; j < scoreBuffer.length; ++j) {
                    if (scoreBuffer[j] > scoreBuffer[optScoreIndex]) optScoreIndex = j;
                }
            }
            patterns.add(new IsotopePattern(formula, scoreBuffer[optScoreIndex], allPatternVariants[optScoreIndex]));
        }
        patterns.sort(Comparator.reverseOrder());
        return patterns;
    }


    public List<IsotopePatternScorer> getIsotopePatternScorers() {
        return isotopePatternScorers;
    }

    public void setIsotopePatternScorers(List<IsotopePatternScorer> isotopePatternScorers) {
        this.isotopePatternScorers = isotopePatternScorers;
    }

    public DecomposerCache getDecomposer() {
        return decomposer;
    }

    public void setDecomposer(DecomposerCache decomposer) {
        this.decomposer = decomposer;
    }

    public IsotopicDistribution getIsotopicDistribution() {
        return isotopicDistribution;
    }

    public void setIsotopicDistribution(IsotopicDistribution isotopicDistribution) {
        this.isotopicDistribution = isotopicDistribution;
    }
}
