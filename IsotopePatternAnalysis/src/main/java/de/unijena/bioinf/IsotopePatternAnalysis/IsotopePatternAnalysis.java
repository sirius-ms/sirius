package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameterized;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.AlreadyExtracted;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.PatternExtractor;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.IsotopePatternScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.LogNormDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.IntensityDependency;
import de.unijena.bioinf.IsotopePatternAnalysis.util.PiecewiseLinearFunctionIntensityDependency;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.addOffset;
import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.normalize;

public class IsotopePatternAnalysis implements Parameterized {

    private List<IsotopePatternScorer> isotopePatternScorers;
    private double cutoff;
    private double intensityOffset;
    private DecomposerCache decomposer;
    private PatternExtractor patternExtractor;
    private IsotopicDistribution isotopicDistribution;
    private MeasurementProfile defaultProfile;

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
        if (document.hasKeyInDictionary(dictionary, "patternExtractor"))
            setPatternExtractor((PatternExtractor) helper.unwrap(document, document.getFromDictionary(dictionary, "patternExtractor")));
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
        document.addToDictionary(dictionary, "patternExtractor", helper.wrap(document, patternExtractor));
        document.addToDictionary(dictionary, "intensityOffset", intensityOffset);

    }

    public static <G, D, L> IsotopePatternAnalysis loadFromProfile(DataDocument<G, D, L> document, G value) {
        final ParameterHelper helper = ParameterHelper.getParameterHelper();
        final D dict = document.getDictionary(value);
        if (!document.hasKeyInDictionary(dict, "IsotopePatternAnalysis"))
            throw new IllegalArgumentException("No field 'IsotopePatternAnalysis' in profile");
        final IsotopePatternAnalysis analyzer = (IsotopePatternAnalysis)helper.unwrap(document,
                document.getFromDictionary(dict, "IsotopePatternAnalysis"));
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
        document.addDictionaryToDictionary(dict, "IsotopePatternAnalysis", fpa);
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

    IsotopePatternAnalysis() {
        this.isotopePatternScorers = new ArrayList<IsotopePatternScorer>();
        this.decomposer = new DecomposerCache();
        this.patternExtractor = new AlreadyExtracted();
        this.isotopicDistribution = PeriodicTable.getInstance().getDistribution();
        this.cutoff = 0.01d;
        this.intensityOffset = 0d;
    }

    public static IsotopePatternAnalysis defaultAnalyzer() {
        final IsotopePatternAnalysis analyzer = new IsotopePatternAnalysis();
        double offset = 1.323d;
        analyzer.intensityOffset = 0.178/100d;
        analyzer.isotopePatternScorers.add(new MassDeviationScorer(new PiecewiseLinearFunctionIntensityDependency(new double[]{0.15, 0.1}, new double[]{
                1.0, 1.5
        })));
        analyzer.isotopePatternScorers.add(new LogNormDistributedIntensityScorer(3, new PiecewiseLinearFunctionIntensityDependency(new double[]{1.0, 0.3, 0.15, 0.03}, new double[]{
                0.0075, 0.017, 0.05, 0.07
        })));
        return analyzer;
    }

    public double getIntensityOffset() {
        return intensityOffset;
    }

    public void setIntensityOffset(double intensityOffset) {
        this.intensityOffset = intensityOffset;
    }

    public List<IsotopePattern> deisotope(MsExperiment experiment) {
        final List<IsotopePattern> patterns = patternExtractor.extractPattern(experiment.getMergedMs1Spectrum()); // TODO: Extra class IsotopePattern
        final List<IsotopePattern> candidates = new ArrayList<IsotopePattern>();
        for (IsotopePattern pattern : patterns) {
            candidates.add(deisotope(experiment, pattern));
        }
        return candidates;
    }

    public IsotopePattern deisotope(MsExperiment experiment, IsotopePattern pattern) {
        final Ionization ion = experiment.getIonization();
        final ArrayList<ScoredMolecularFormula> result = new ArrayList<ScoredMolecularFormula>();
        final List<MolecularFormula> molecules = decomposer.getDecomposer(experiment.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet()).decomposeToFormulas(
                ion.subtractFromMass(pattern.getMonoisotopicMass()), experiment.getMeasurementProfile().getAllowedMassDeviation(), null, experiment.getMeasurementProfile().getFormulaConstraints().getFilters().get(0)
        ); // TODO: Fix
        final double[] scores = scoreFormulas(new ChargedSpectrum(pattern.getPattern(), ion), molecules, experiment);
        for (int i=0; i < scores.length; ++i) {
            result.add(new ScoredMolecularFormula(molecules.get(i), scores[i]));
        }
        return new IsotopePattern(pattern.getPattern(), result);
    }

    public double[] scoreFormulas(ChargedSpectrum extractedSpectrum, List<MolecularFormula> formulas, MsExperiment experiment) {
        final PatternGenerator generator = new PatternGenerator(isotopicDistribution, extractedSpectrum.getIonization(), Normalization.Sum(1));
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(extractedSpectrum.getNeutralMassSpectrum());
        normalize(spec, Normalization.Sum(1));
        if (intensityOffset != 0d) {
            addOffset(spec, 0d, intensityOffset);
        }
        normalize(spec, Normalization.Sum(1));
        while (spec.getIntensityAt(spec.size()-1) < cutoff) spec.removePeakAt(spec.size()-1);
        normalize(spec, Normalization.Sum(1));
        final Spectrum<Peak> measuredSpectrum = new SimpleSpectrum(spec);
        final double monoIsotopicMass = spec.getMzAt(0);
        final ArrayList<ScoredMolecularFormula> scoredFormulas = new ArrayList<ScoredMolecularFormula>(formulas.size());
        final double[] scores = new double[formulas.size()];
        int k=0;
        for (MolecularFormula f : formulas) {
            final Spectrum<Peak> theoreticalSpectrum= generator.generatePattern(f, spec.size()+1).getNeutralMassSpectrum();
            if (theoreticalSpectrum.size() < spec.size()) {
                // TODO: Just a Workaround!!! Find something better
                final SimpleMutableSpectrum workaround = new SimpleMutableSpectrum(measuredSpectrum);
                while (theoreticalSpectrum.size() < workaround.size()) workaround.removePeakAt(workaround.size()-1);
                normalize(workaround, Normalization.Sum(1));
                double score = 0d;
                for (IsotopePatternScorer scorer : isotopePatternScorers)
                    score += scorer.score(workaround, theoreticalSpectrum, Normalization.Sum(1), experiment);
                // add missing peak scores too all deleted peaks if MissingPeakScorer is given
                for (int i=theoreticalSpectrum.size(); i < spec.size(); ++i) {
                    score -= spec.getIntensityAt(i)*100;
                }
                scores[k++] = score;
            } else {
                double score = 0d;
                for (IsotopePatternScorer scorer : isotopePatternScorers)
                    score += scorer.score(measuredSpectrum, theoreticalSpectrum, Normalization.Sum(1), experiment);
                scores[k++] = score;
            }
        }
        return scores;
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

    public PatternExtractor getPatternExtractor() {
        return patternExtractor;
    }

    public void setPatternExtractor(PatternExtractor patternExtractor) {
        this.patternExtractor = patternExtractor;
    }

    public IsotopicDistribution getIsotopicDistribution() {
        return isotopicDistribution;
    }

    public void setIsotopicDistribution(IsotopicDistribution isotopicDistribution) {
        this.isotopicDistribution = isotopicDistribution;
    }
}
