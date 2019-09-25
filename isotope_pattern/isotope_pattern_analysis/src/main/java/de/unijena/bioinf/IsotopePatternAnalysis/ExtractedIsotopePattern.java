package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.HashMap;

public class ExtractedIsotopePattern implements DataAnnotation {

    protected SimpleSpectrum pattern;
    protected HashMap<MolecularFormula, IsotopePattern> explanations;

    private final static ExtractedIsotopePattern NONE = new ExtractedIsotopePattern(new SimpleSpectrum(new double[0], new double[0]));

    public static ExtractedIsotopePattern none() {
        return NONE;
    }

    public ExtractedIsotopePattern(SimpleSpectrum spectrum) {
        this(spectrum, new HashMap<MolecularFormula, IsotopePattern>());
    }

    public ExtractedIsotopePattern(SimpleSpectrum pattern, HashMap<MolecularFormula, IsotopePattern> explanations) {
        this.pattern = pattern;
        this.explanations = explanations;
    }

    public boolean hasPatternWithAtLeastTwoPeaks() {
        return pattern!=null && pattern.size()>1;
    }

    public boolean hasPattern() {
        return pattern!=null && !pattern.isEmpty();
    }

    public SimpleSpectrum getPattern() {
        return pattern;
    }

    public HashMap<MolecularFormula, IsotopePattern> getExplanations() {
        return explanations;
    }
}
