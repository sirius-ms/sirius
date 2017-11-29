package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;

import java.util.HashMap;

public class ExtractedIsotopePattern {

    protected SimpleSpectrum pattern;
    protected HashMap<MolecularFormula, IsotopePattern> explanations;

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
        return pattern!=null;
    }

    public SimpleSpectrum getPattern() {
        return pattern;
    }

    public HashMap<MolecularFormula, IsotopePattern> getExplanations() {
        return explanations;
    }
}
