package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.PatternExtractor;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

import java.util.ArrayList;
import java.util.List;

public class IsotopePatternAnalysis {

    private List<IsotopePatternScorer<Peak, Spectrum<Peak>>> isotopePatternScorers;
    private DecomposerCache decomposer;
    private PatternExtractor patternExtractor;
    private IsotopicDistribution isotopicDistribution;

    IsotopePatternAnalysis() {
        this.isotopePatternScorers = new ArrayList<IsotopePatternScorer<Peak, Spectrum<Peak>>>();
        this.decomposer = new DecomposerCache();
        this.patternExtractor = null;
        this.isotopicDistribution = PeriodicTable.getInstance().getDistribution();
    }

    public static IsotopePatternAnalysis defaultAnalyzer() {
        final IsotopePatternAnalysis analyzer = new IsotopePatternAnalysis();
        analyzer.isotopePatternScorers.add(new MassDeviationScorer<Peak, Spectrum<Peak>>());

    }

}
