package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.ChargedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.AlreadyExtracted;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.PatternExtractor;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.LogNormDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MissingPeakScorer;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabetWrapper;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.MassDecomposer.ValenceBoundary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.*;

public class DeIsotope {

    private IsotopePatternScorer<Peak, Spectrum<Peak>> isotopePatternScorer;
    private MassToFormulaDecomposer decomposer;
    private PatternExtractor patternExtractor;
    private IsotopicDistribution isotopicDistribution;
    private Map<Element, Interval> elementBoundary;
    private ChemicalAlphabet alphabet;
    private double intensityOffset;
    private double intensityTreshold;
    private double ppm, alpha, absError;

    public DeIsotope(double ppm) {
        this(ppm, 100*1e-6*ppm);
    }

    public DeIsotope(double ppm, double absError) {
        this(ppm, absError,  new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S")));
    }

    public DeIsotope(double ppm, double absError, ChemicalAlphabet alphabet) {
        this(ppm, absError, alphabet, PeriodicTable.getInstance().getDistribution());
    }

    public DeIsotope(double ppm, double absError, ChemicalAlphabet alphabet, IsotopicDistribution distribution) {
        this(ppm, absError, alphabet, distribution, 3, 3, 0.1, 0.9, 1.0, 1.3, 50, 0.002);
    }

    public DeIsotope(double ppm, double absError, ChemicalAlphabet alphabet, IsotopicDistribution distribution,
                     double massPenalty, double intensityPenalty, double alpha, double beta, double relativeMassErrorFull,
                     double relativeMassErrorZero, double lambdaMissingPeak, double intensityOffset) {
        this.decomposer = new MassToFormulaDecomposer(alphabet);
        this.patternExtractor = new AlreadyExtracted();
        this.isotopicDistribution = distribution;
        this.isotopePatternScorer = new PatternScoreList(new MassDeviationScorer(massPenalty, ppm*relativeMassErrorFull, ppm*relativeMassErrorZero),
                new LogNormDistributedIntensityScorer(intensityPenalty, alpha, beta), new MissingPeakScorer(lambdaMissingPeak));
        this.alphabet = alphabet;
        this.elementBoundary = alphabet.toMap();
        this.intensityOffset = intensityOffset;
        this.alpha = alpha;
        this.ppm = ppm;
        this.absError = absError;
    }

    public double getIntensityTreshold() {
        return intensityTreshold;
    }

    public void setIntensityTreshold(double intensityTreshold) {
        this.intensityTreshold = intensityTreshold;
    }

    public double getIntensityOffset() {
        return intensityOffset;
    }

    public void setIntensityOffset(double intensityOffset) {
        this.intensityOffset = intensityOffset;
    }

    public Map<Element, Interval> getElementBoundary() {
        return elementBoundary;
    }

    public List<Spectrum<Peak>> extractPatterns(Spectrum<Peak> spectrum) {
        return patternExtractor.extractPattern(spectrum);
    }

    public List<ScoredMolecularFormula> deisotope(ChargedSpectrum extractedSpectrum) {
        final double monoIsotopicMass = extractedSpectrum.getPeakAt(getIndexOfPeakWithMinimalMass(extractedSpectrum)).getNeutralMass();
        final List<MolecularFormula> formulas = decomposer.decomposeToFormulas(monoIsotopicMass, new Deviation(ppm, absError),
                new ValenceBoundary<Element>(new ChemicalAlphabetWrapper(alphabet)).getMapFor(monoIsotopicMass, elementBoundary));
        final ArrayList<ScoredMolecularFormula> scoredFormulas = new ArrayList<ScoredMolecularFormula>(formulas.size());
        final double[] scores = scoreFormulas(extractedSpectrum, formulas);
        for (int i=0; i < scores.length; ++i) {
            scoredFormulas.add(new ScoredMolecularFormula(formulas.get(i), scores[i]));
        }
        Collections.sort(scoredFormulas, Collections.reverseOrder());
        return scoredFormulas;
    }

    public double[] scoreFormulas(ChargedSpectrum extractedSpectrum, List<MolecularFormula> formulas) {
        final PatternGenerator generator = new PatternGenerator(isotopicDistribution, extractedSpectrum.getIonization(), Normalization.Sum(1));
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(extractedSpectrum.getNeutralMassSpectrum());
        normalize(spec, Normalization.Sum(1));
        addOffset(spec, 0, intensityOffset);
        normalize(spec, Normalization.Sum(1));
        while (spec.getIntensityAt(spec.size()-1) < intensityTreshold) spec.removePeakAt(spec.size()-1);
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
                double score = isotopePatternScorer.score(workaround, theoreticalSpectrum, Normalization.Sum(1));
                // add missing peak scores too all deleted peaks if MissingPeakScorer is given
                for (int i=theoreticalSpectrum.size(); i < spec.size(); ++i) {
                    score -= spec.getIntensityAt(i)*100;
                }
                scores[k++] = score;
            } else {
                final double score = isotopePatternScorer.score(measuredSpectrum, theoreticalSpectrum, Normalization.Sum(1));
                scores[k++] = score;
            }
        }
        return scores;
    }

    public double scoreFormula(ChargedSpectrum extractedSpectrum, MolecularFormula formula) {
        final PatternGenerator generator = new PatternGenerator(isotopicDistribution, extractedSpectrum.getIonization(), Normalization.Sum(1));
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(extractedSpectrum.getNeutralMassSpectrum());
        normalize(spec, Normalization.Sum(1));
        addOffset(spec, 0, intensityOffset);
        normalize(spec, Normalization.Sum(1));
        while (spec.getIntensityAt(spec.size()-1) < intensityTreshold) spec.removePeakAt(spec.size()-1);
        normalize(spec, Normalization.Sum(1));
        final Spectrum<Peak> measuredSpectrum = new SimpleSpectrum(spec);
        final double monoIsotopicMass = spec.getMzAt(0);;
        final Spectrum<Peak> theoreticalSpectrum = generator.generatePattern(formula, spec.size()+1).getNeutralMassSpectrum();
        final double score = isotopePatternScorer.score(measuredSpectrum, theoreticalSpectrum, Normalization.Sum(1));
        return score;
    }

    public IsotopicDistribution getIsotopicDistribution() {
        return isotopicDistribution;
    }

    public void setIsotopicDistribution(IsotopicDistribution isotopicDistribution) {
        this.isotopicDistribution = isotopicDistribution;
    }

    public IsotopePatternScorer<Peak, Spectrum<Peak>> getIsotopePatternScorer() {
        return isotopePatternScorer;
    }

    public void setIsotopePatternScorer(IsotopePatternScorer<Peak, Spectrum<Peak>> isotopePatternScorer) {
        this.isotopePatternScorer = isotopePatternScorer;
    }

    public MassToFormulaDecomposer getDecomposer() {
        return decomposer;
    }

    public void setDecomposer(MassToFormulaDecomposer decomposer) {
        this.decomposer = decomposer;
    }

    public PatternExtractor getPatternExtractor() {
        return patternExtractor;
    }

    public void setPatternExtractor(PatternExtractor patternExtractor) {
        this.patternExtractor = patternExtractor;
    }
}
