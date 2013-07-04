package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.MsExperiment;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.AlreadyExtracted;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.PatternExtractor;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.IsotopePatternScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.LogNormDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.PiecewiseLinearFunctionIntensityDependency;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;

import java.util.ArrayList;
import java.util.List;

import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.normalize;

public class IsotopePatternAnalysis {

    private List<IsotopePatternScorer> isotopePatternScorers;
    private double cutoff;
    private DecomposerCache decomposer;
    private PatternExtractor patternExtractor;
    private IsotopicDistribution isotopicDistribution;

    IsotopePatternAnalysis() {
        this.isotopePatternScorers = new ArrayList<IsotopePatternScorer>();
        this.decomposer = new DecomposerCache();
        this.patternExtractor = new AlreadyExtracted();
        this.isotopicDistribution = PeriodicTable.getInstance().getDistribution();
        this.cutoff = 0.01d;
    }

    public static IsotopePatternAnalysis defaultAnalyzer() {
        final IsotopePatternAnalysis analyzer = new IsotopePatternAnalysis();
        analyzer.isotopePatternScorers.add(new MassDeviationScorer(3, new PiecewiseLinearFunctionIntensityDependency(new double[]{0.8, 0.5, 0.2, 0.1, 0.05, 0.01}, new double[]{
                1, 1, 1.2, 1.5, 2, 2.5
        })));
        analyzer.isotopePatternScorers.add(new LogNormDistributedIntensityScorer(3, new PiecewiseLinearFunctionIntensityDependency(new double[]{0.8, 0.5, 0.2, 0.1, 0.05, 0.01}, new double[]{
                0.08, 0.12, 0.25, 0.5, 1.2, 2d
        })));
        return analyzer;
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
        // TODO: Add offset in MeasurementProfile
        /*
        normalize(spec, Normalization.Sum(1));
        addOffset(spec, 0, intensityOffset);
         */
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
