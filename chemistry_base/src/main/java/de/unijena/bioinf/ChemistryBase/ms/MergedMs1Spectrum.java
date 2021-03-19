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

package de.unijena.bioinf.ChemistryBase.ms;


import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

public class MergedMs1Spectrum implements ProcessedInputAnnotation {

    public final boolean peaksAreCorrelated;
    public final SimpleSpectrum mergedSpectrum;

    private final static MergedMs1Spectrum EMPTY = new MergedMs1Spectrum(false, new SimpleSpectrum(new double[0], new double[0]));

    public static MergedMs1Spectrum empty() {
        return EMPTY;
    }

    public MergedMs1Spectrum(boolean peaksAreCorrelated, SimpleSpectrum mergedSpectrum) {
        this.peaksAreCorrelated = peaksAreCorrelated;
        this.mergedSpectrum = mergedSpectrum;
    }

    public boolean isEmpty() {
        return mergedSpectrum.size()==0;
    }

    public boolean isCorrelated() {
        return peaksAreCorrelated;
    }
}
