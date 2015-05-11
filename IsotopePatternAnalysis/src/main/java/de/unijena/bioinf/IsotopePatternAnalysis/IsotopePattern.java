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

import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.util.Collections;
import java.util.List;

public class IsotopePattern {

    private final SimpleSpectrum pattern;
    private final List<ScoredMolecularFormula> candidates;
    private final double bestScore;

    public IsotopePattern(Spectrum<Peak> pattern) {
        this(pattern, Collections.<ScoredMolecularFormula>emptyList());
    }

    public IsotopePattern(Spectrum<Peak> pattern, List<ScoredMolecularFormula> candidates) {
        this.pattern = new SimpleSpectrum(pattern);
        this.candidates = candidates;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ScoredMolecularFormula f : candidates) bestScore = Math.max(f.getScore(), bestScore);
        this.bestScore = bestScore;
    }

    public double getBestScore() {
        return bestScore;
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
