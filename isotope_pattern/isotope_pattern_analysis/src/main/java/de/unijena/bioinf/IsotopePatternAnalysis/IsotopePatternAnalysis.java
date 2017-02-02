/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.IsotopePatternAnalysis;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.IsotopePatternScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.NormalDistributedIntensityScorer;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;

import java.util.*;

import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.addOffset;
import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.normalize;

public class IsotopePatternAnalysis implements Parameterized {

    public static final String ANALYZER_NAME = "IsotopePatternAnalysis";

    private List<IsotopePatternScorer> isotopePatternScorers;
    private double cutoff;
    private double intensityOffset;
    private DecomposerCache decomposer;
    private IsotopicDistribution isotopicDistribution;
    private IsotopePatternGenerator patternGenerator;
    private MutableMeasurementProfile defaultProfile;

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "patternScorers")) {
            final Iterator<G> scorers = document.iteratorOfList(document.getListFromDictionary(dictionary, "patternScorers"));
            while (scorers.hasNext()) {
                getIsotopePatternScorers().add((IsotopePatternScorer) helper.unwrap(document, scorers.next()));
            }
        }
        if (document.hasKeyInDictionary(dictionary, "cutoff"))
            setCutoff(document.getDoubleFromDictionary(dictionary, "cutoff"));
        if (document.hasKeyInDictionary(dictionary, "intensityOffset"))
            setIntensityOffset(document.getDoubleFromDictionary(dictionary, "intensityOffset"));
        if (document.hasKeyInDictionary(dictionary, "isotopes"))
            setIsotopicDistribution((IsotopicDistribution) helper.unwrap(document, document.getFromDictionary(dictionary, "isotopes")));
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        exportParameters(helper, document, dictionary, true);
    }


    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary, boolean withProfile) {
        document.addToDictionary(dictionary, "cutoff", cutoff);
        if (withProfile && defaultProfile!=null) document.addToDictionary(dictionary, "default", helper.wrap(document, defaultProfile));
        // export isotope distribution for relevant elements
        final ChemicalAlphabet alphabet;
        if (defaultProfile != null) {
            alphabet = defaultProfile.getFormulaConstraints().getChemicalAlphabet();
        } else {
            alphabet = ChemicalAlphabet.getExtendedAlphabet();
        }
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
        final IsotopePatternAnalysis analyzer = (IsotopePatternAnalysis)helper.unwrap(document,
                document.getFromDictionary(dict, ANALYZER_NAME));
        if (document.hasKeyInDictionary(dict, "profile")) {
            final MeasurementProfile prof = ((MeasurementProfile) helper.unwrap(document, document.getFromDictionary(dict, "profile")));
            if (analyzer.defaultProfile==null) analyzer.defaultProfile=new MutableMeasurementProfile(prof);
            else analyzer.defaultProfile = new MutableMeasurementProfile(MutableMeasurementProfile.merge(prof, analyzer.defaultProfile));
        }
        return analyzer;
    }

    public <G, D, L> void writeToProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        final D fpa = document.newDictionary();
        exportParameters(helper, document, fpa);
        document.addToDictionary(fpa, "$name", ANALYZER_NAME );
        document.addDictionaryToDictionary(dict, ANALYZER_NAME, fpa);
        if (document.hasKeyInDictionary(dict, "profile")) {
            final MeasurementProfile otherProfile = (MeasurementProfile) helper.unwrap(document, document.getFromDictionary(dict, "profile"));
            if (!otherProfile.equals(defaultProfile)) {
                if (defaultProfile != null) {
                    final D profDict = document.newDictionary();
                    new MutableMeasurementProfile(defaultProfile).exportParameters(helper, document, profDict);
                    document.addDictionaryToDictionary(fpa, "default", profDict);
                }
            }
        } else if (defaultProfile!=null){
            final D profDict = document.newDictionary();
            new MutableMeasurementProfile(defaultProfile).exportParameters(helper, document, profDict);
            document.addDictionaryToDictionary(dict, "profile", profDict);
        }
    }

    public IsotopePatternAnalysis() {
        this.isotopePatternScorers = new ArrayList<IsotopePatternScorer>();
        this.decomposer = new DecomposerCache();
        this.isotopicDistribution = PeriodicTable.getInstance().getDistribution();
        this.cutoff = 0.01d;
        this.intensityOffset = 0d;
        this.patternGenerator = new FastIsotopePatternGenerator(isotopicDistribution, Normalization.Max(1d));
    }

    public static IsotopePatternAnalysis defaultAnalyzer() {
        final PeriodicTable T = PeriodicTable.getInstance();
        final IsotopePatternAnalysis analyzer = new IsotopePatternAnalysis();
        double offset = 1.323d;
        analyzer.intensityOffset = 0d;
        analyzer.isotopePatternScorers.add(new MassDifferenceDeviationScorer());
        analyzer.isotopePatternScorers.add(new NormalDistributedIntensityScorer());
        final FormulaConstraints constr =  new FormulaConstraints(new ChemicalAlphabet(T.getAllByName("C", "H",
                "N", "O", "P", "S", "Cl", "Na")), null);
        constr.setUpperbound(T.getByName("Cl"), 1);
        constr.setUpperbound(T.getByName("Na"), 1);
        constr.setUpperbound(T.getByName("P"), 3);
        constr.setUpperbound(T.getByName("S"), 3);
        constr.setUpperbound(T.getByName("N"), 10);
        constr.setUpperbound(T.getByName("O"), 25);

        analyzer.defaultProfile = new MutableMeasurementProfile(new Deviation(10), new Deviation(5), new Deviation(5), new Deviation(2.5), constr, 0.008d, 0.02d);
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
        return extractPattern(experiment, getDefaultProfile(), targetMz);
    }

    public SimpleSpectrum extractPattern(Ms2Experiment experiment, MeasurementProfile profile, double targetMz) {
        final Spectrum<Peak> s = experiment.getMergedMs1Spectrum();
        return extractPattern(s!=null ? s : experiment.getMs1Spectra().get(0), profile, targetMz);
    }

    public SimpleSpectrum extractPattern(Spectrum<Peak> ms1Spec, MeasurementProfile profile, double targetMz) {
        // extract all isotope peaks starting from the given target mz
        final ChemicalAlphabet stdalphabet = ChemicalAlphabet.getExtendedAlphabet();
        final Spectrum<Peak> massOrderedSpectrum = Spectrums.getMassOrderedSpectrum(ms1Spec);
        final ArrayList<SimpleSpectrum> patterns = new ArrayList<SimpleSpectrum>();
        final int index = Spectrums.mostIntensivePeakWithin(massOrderedSpectrum, targetMz, profile.getAllowedMassDeviation());
        if (index < 0) return null;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        spec.addPeak(massOrderedSpectrum.getPeakAt(index));
        // add additional peaks
        for (int k=1; k <= 5; ++k) {
            final Range<Double> nextMz = PeriodicTable.getInstance().getIsotopicMassWindow(stdalphabet, profile.getAllowedMassDeviation(), spec.getMzAt(0), k);
            final double a = nextMz.lowerEndpoint();
            final double b = nextMz.upperEndpoint();
            final double m = a+(b-a)/2d;
            final double startPoint = a - profile.getStandardMassDifferenceDeviation().absoluteFor(a);
            final double endPoint = b + profile.getStandardMassDifferenceDeviation().absoluteFor(b);
            final int nextIndex = Spectrums.indexOfFirstPeakWithin(massOrderedSpectrum, startPoint, endPoint);
            if (nextIndex < 0) break;
            double mzBuffer = 0d;
            double intensityBuffer = 0d;
            for (int i=nextIndex; i < massOrderedSpectrum.size(); ++i) {
                final double mz = massOrderedSpectrum.getMzAt(i);
                if (mz > endPoint) break;
                final double intensity = massOrderedSpectrum.getIntensityAt(i);
                mzBuffer += mz*intensity;
                intensityBuffer += intensity;
            }
            mzBuffer /= intensityBuffer;
            spec.addPeak(mzBuffer, intensityBuffer);
        }
        return new SimpleSpectrum(spec);
    }

    private MeasurementProfile getProfile(MeasurementProfile measurementProfile) {
        if (measurementProfile==null) return defaultProfile;
        return MutableMeasurementProfile.merge(defaultProfile, measurementProfile);
    }



    public List<IsotopePattern> deisotope(Ms2Experiment experiment, MeasurementProfile profile, List<MolecularFormula> formulas) {
        if (experiment.getMs1Spectra().isEmpty() && experiment.getMergedMs1Spectrum()==null) return new ArrayList<>();
        final SimpleSpectrum pattern = extractPattern(experiment, getProfile(profile), experiment.getIonMass());
        if (pattern==null) return Collections.emptyList();
        return scoreFormulas(pattern, formulas, experiment, profile);
    }

    public List<IsotopePattern> deisotope(Ms2Experiment experiment, MeasurementProfile profile) {
        if (experiment.getMs1Spectra().isEmpty() && experiment.getMergedMs1Spectrum()==null) return new ArrayList<>();
        final SimpleSpectrum pattern = extractPattern(experiment, getProfile(profile), experiment.getIonMass());
        if (pattern==null) return Collections.emptyList();
        final PrecursorIonType ionization = experiment.getPrecursorIonType();
        if (ionization.isIonizationUnknown()) {
            // try different ionization types
            final List<IsotopePattern> ionFormulas = new ArrayList<IsotopePattern>();
            final int charge = ionization.getCharge();
            final Iterable<Ionization> ionModes = PeriodicTable.getInstance().getKnownIonModes(charge);
            for (Ionization ion : ionModes) {
                final List<MolecularFormula> formulas = decomposer.getDecomposer(profile.getFormulaConstraints().getChemicalAlphabet()).decomposeToFormulas(ion.subtractFromMass(pattern.getMzAt(0)), profile.getAllowedMassDeviation(), profile.getFormulaConstraints());
                ionFormulas.addAll(scoreFormulas(pattern, formulas, experiment, profile, PrecursorIonType.getPrecursorIonType(ion)));
            }
            Collections.sort(ionFormulas, Scored.<MolecularFormula>desc());
            return ionFormulas;
        } else {
            // use given ionization
            final List<Scored<MolecularFormula>> neutralFormulas = new ArrayList<Scored<MolecularFormula>>();
            final List<MolecularFormula> formulas = decomposer.getDecomposer(profile.getFormulaConstraints().getChemicalAlphabet()).decomposeToFormulas(ionization.precursorMassToNeutralMass(pattern.getMzAt(0)), profile.getAllowedMassDeviation(), profile.getFormulaConstraints());
            return scoreFormulas(pattern, formulas, experiment, profile);
        }
    }

    public List<IsotopePattern> scoreFormulas(SimpleSpectrum extractedSpectrum, List<MolecularFormula> formulas, Ms2Experiment experiment, MeasurementProfile profile) {
        return scoreFormulas(extractedSpectrum, formulas, experiment, profile, experiment.getPrecursorIonType());
    }

    public List<IsotopePattern> scoreFormulas(SimpleSpectrum extractedSpectrum, List<MolecularFormula> formulas, Ms2Experiment experiment, MeasurementProfile profile, PrecursorIonType ion) {
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(extractedSpectrum);
        normalize(spec, Normalization.Sum(1d));
        if (intensityOffset != 0d) {
            addOffset(spec, 0d, intensityOffset);
            normalize(spec, Normalization.Sum(1d));
        }

        if (spec.getIntensityAt(0) < cutoff){
            //intensity of first peak is below cutoff, cannot score
            return new ArrayList<>();
        }
        while (spec.getIntensityAt(spec.size()-1) < cutoff) spec.removePeakAt(spec.size()-1);
        normalize(spec, Normalization.Max(1));
        final Spectrum<Peak> measuredSpectrum = new SimpleSpectrum(spec);
        final ArrayList<IsotopePattern> patterns = new ArrayList<>(formulas.size());
        final SimpleSpectrum[] allPatternVariants = new SimpleSpectrum[measuredSpectrum.size()];
        {
            final SimpleMutableSpectrum mut = new SimpleMutableSpectrum(allPatternVariants.length);
            for (int k=0; k < allPatternVariants.length; ++k) {
                mut.addPeak(measuredSpectrum.getMzAt(k), measuredSpectrum.getIntensityAt(k));
                allPatternVariants[k] = new SimpleSpectrum(mut);
            }
        }
        int k=0;
        final double[] scoreBuffer = new double[allPatternVariants.length];
        for (MolecularFormula f : formulas) {
            Arrays.fill(scoreBuffer, 0d);
            f = ion.neutralMoleculeToMeasuredNeutralMolecule(f);
            Spectrum<Peak> measuredOne = measuredSpectrum;
            Spectrum<Peak> theoreticalSpectrum = patternGenerator.simulatePattern(f, ion.getIonization());
            if (theoreticalSpectrum.size()==0) continue;
            if (theoreticalSpectrum.size() > 10)
                theoreticalSpectrum = Spectrums.getNormalizedSpectrum(Spectrums.subspectrum(theoreticalSpectrum, 0, 10), Normalization.Max(1d));
            if (measuredSpectrum.size() > theoreticalSpectrum.size())
                measuredOne = Spectrums.getNormalizedSpectrum(Spectrums.subspectrum(measuredSpectrum, 0, theoreticalSpectrum.size()), Normalization.Max(1d));
            for (IsotopePatternScorer scorer : isotopePatternScorers) {
                scorer.score(scoreBuffer, measuredOne, theoreticalSpectrum, Normalization.Max(1), experiment, profile);
            }
            int optScoreIndex = 0;
            for (int j=0; j < scoreBuffer.length; ++j) {
                if (scoreBuffer[j] > scoreBuffer[optScoreIndex]) optScoreIndex=j;
            }
            patterns.add(new IsotopePattern(f, scoreBuffer[optScoreIndex], allPatternVariants[optScoreIndex]));
        }
        Collections.sort(patterns, Scored.<MolecularFormula>desc());
        return patterns;
    }

    public MutableMeasurementProfile getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(MutableMeasurementProfile defaultProfile) {
        this.defaultProfile = defaultProfile;
    }

    public List<IsotopePatternScorer> getIsotopePatternScorers() {
        return isotopePatternScorers;
    }

    public void setIsotopePatternScorers(List<IsotopePatternScorer> isotopePatternScorers) {
        this.isotopePatternScorers = isotopePatternScorers;
    }

    public double getCutoff() {
        return cutoff;
    }

    public void setCutoff(double cutoff) {
        this.cutoff = cutoff;
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
