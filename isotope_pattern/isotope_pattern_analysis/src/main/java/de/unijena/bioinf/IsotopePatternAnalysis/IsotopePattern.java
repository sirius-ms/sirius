
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.IsotopePatternAnalysis;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

public final class IsotopePattern extends Scored<MolecularFormula>  implements TreeAnnotation  {

    private final SimpleSpectrum pattern;

    public IsotopePattern(MolecularFormula candidate, double score, SimpleSpectrum pattern) {
        super(candidate, score);
        this.pattern = pattern;
    }

    public IsotopePattern withScore(double newScore) {
        return new IsotopePattern(getCandidate(),newScore,pattern);
    }

    public SimpleSpectrum getPattern() {
        return pattern;
    }

    public double getMonoisotopicMass() {
        return pattern.getMzAt(0);
    }

    @Override
    public String toString() {
        return getCandidate().toString() + "(" + getScore() + ") <- " + getPattern().toString();
    }
}
