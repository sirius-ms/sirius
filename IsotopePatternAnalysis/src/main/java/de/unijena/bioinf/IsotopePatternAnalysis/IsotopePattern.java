package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.util.Collections;
import java.util.List;

public class IsotopePattern {

    private final SimpleSpectrum pattern;
    private final List<ScoredMolecularFormula> candidates;

    public IsotopePattern(Spectrum<Peak> pattern) {
        this(pattern, Collections.<ScoredMolecularFormula>emptyList());
    }

    public IsotopePattern(Spectrum<Peak> pattern, List<ScoredMolecularFormula> candidates) {
        this.pattern = new SimpleSpectrum(pattern);
        this.candidates = candidates;
    }

    public List<ScoredMolecularFormula> getCandidates() {
        return candidates;
    }

    public SimpleSpectrum getPattern() {
        return pattern;
    }

    public double getMonoisotopicMass() {
        return pattern.getMzAt(0);
    }
}
