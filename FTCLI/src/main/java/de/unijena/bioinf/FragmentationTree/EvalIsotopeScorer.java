package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.PatternGenerator;

/**
 * TODO: Push into separate branch "newScores2013"
 */
public class EvalIsotopeScorer implements MolecularFormulaScorer{

    private MolecularFormula correctFormula;
    private SimpleSpectrum correctPattern;

    public EvalIsotopeScorer() {
    }

    public EvalIsotopeScorer(MolecularFormula correctFormula) {
        setCorrectFormula(correctFormula);
    }

    @Override
    public double score(MolecularFormula formula) {
        final SimpleSpectrum spec = simulatePattern(formula);
        final int minN = Math.min(spec.size(), correctPattern.size());
        final int maxN = Math.max(spec.size(), correctPattern.size());
        double sum = 0d;
        for (int i=0; i < minN; ++i) {
            sum += Math.abs(correctPattern.getIntensityAt(i)-spec.getIntensityAt(i));
        }
        for (int i=minN; i < maxN; ++i) {
            if (i < spec.size()) sum += spec.getIntensityAt(i);
            if (i < correctPattern.size()) sum += correctPattern.getIntensityAt(i);
        }
        return 2d-sum;
    }

    public MolecularFormula getCorrectFormula() {
        return correctFormula;
    }

    public void setCorrectFormula(MolecularFormula correctFormula) {
        this.correctFormula = correctFormula;
        this.correctPattern = simulatePattern(correctFormula);
    }

    private static SimpleSpectrum simulatePattern(MolecularFormula correctFormula) {
        final PatternGenerator gen = new PatternGenerator(Normalization.Sum(1d));
        return new SimpleSpectrum(gen.generatePatternWithTreshold(correctFormula, 0.01d));
    }


}
