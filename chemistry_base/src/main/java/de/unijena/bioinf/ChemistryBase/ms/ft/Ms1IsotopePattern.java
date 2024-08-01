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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.util.Arrays;

/**
 * Isotope pattern coming from MS1
 * TODO: workaround. We have to clean that up
 */
public class Ms1IsotopePattern implements ProcessedInputAnnotation, TreeAnnotation {
    private static final SimpleSpectrum EMPTY_SPECTRUM = new SimpleSpectrum(new double[0], new double[0]);
    private static final Ms1IsotopePattern EMPTY_PATTERN = new Ms1IsotopePattern(EMPTY_SPECTRUM, 0d);

    private final SimpleSpectrum spectrum;
    private final double score;

    public static Ms1IsotopePattern none() {
        return EMPTY_PATTERN;
    }

    public Ms1IsotopePattern(Spectrum<? extends Peak> ms, double score) {
        this.spectrum = (ms.size()==0) ? EMPTY_SPECTRUM : new SimpleSpectrum(ms);
        this.score = score;
    }

    public Ms1IsotopePattern(Peak[] peaks, double score) {
        this.spectrum = peaks.length==0 ? EMPTY_SPECTRUM : Spectrums.from(Arrays.asList(peaks));
        this.score = score;
    }

    public boolean isEmpty() {
        return spectrum.size()==0;
    }


    public Peak[] getPeaks() {
        return Spectrums.extractPeakList(spectrum).toArray(new Peak[0]);
    }

    public double getScore() {
        return score;
    }

    public SimpleSpectrum getSpectrum() {
        return spectrum;
    }
}
