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
