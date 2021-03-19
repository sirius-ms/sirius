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

package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class CosineQuerySpectrum implements Spectrum<Peak> {
    final OrderedSpectrum<Peak> spectrum;
    final SimpleSpectrum inverseSpectrum;
    final double selfSimilarity;
    final double selfSimilarityLosses;
    final double precursorMz;

    private CosineQuerySpectrum(OrderedSpectrum<Peak> spectrum, double precursorMz, SimpleSpectrum inverseSpectrum, double selfSimilarity, double selfSimilarityLosses) {
        this.spectrum = spectrum;
        this.precursorMz = precursorMz;
        this.inverseSpectrum = inverseSpectrum;
        this.selfSimilarity = selfSimilarity;
        this.selfSimilarityLosses = selfSimilarityLosses;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(">parentmass ");buf.append(precursorMz);buf.append('\n');
        for (int k=0; k < spectrum.size(); ++k) {
            buf.append(spectrum.getMzAt(k));
            buf.append('\t');
            buf.append(spectrum.getIntensityAt(k));
            buf.append('\n');
        }
        return buf.toString();
    }

    public double getPrecursorMz() {
        return precursorMz;
    }

    public SimpleSpectrum getInverseSpectrum() {
        return inverseSpectrum;
    }

    public double getSelfSimilarity() {
        return selfSimilarity;
    }

    public double getSelfSimilarityLosses() {
        return selfSimilarityLosses;
    }

    @Override
    public double getMzAt(int index) {
        return spectrum.getMzAt(index);
    }

    @Override
    public double getIntensityAt(int index) {
        return spectrum.getIntensityAt(index);
    }

    @Override
    public Peak getPeakAt(int index) {
        return spectrum.getPeakAt(index);
    }

    public int size() {
        return spectrum.size();
    }

    protected static CosineQuerySpectrum newInstance(OrderedSpectrum<Peak> spectrum, double precursorMz, AbstractSpectralAlignment spectralAlignment) {
        SimpleSpectrum inverseSpectrum = Spectrums.getInversedSpectrum(spectrum, precursorMz);
        double selfSimilarity = spectralAlignment.score(spectrum, spectrum).similarity;
        double selfSimilarityLosses = spectralAlignment.score(inverseSpectrum, inverseSpectrum).similarity;
        return new CosineQuerySpectrum(spectrum, precursorMz, inverseSpectrum, selfSimilarity, selfSimilarityLosses);
    }
    protected static CosineQuerySpectrum newInstanceWithoutLoss(OrderedSpectrum<Peak> spectrum, double precursorMz, AbstractSpectralAlignment spectralAlignment) {
        double selfSimilarity = spectralAlignment.score(spectrum, spectrum).similarity;
        return new CosineQuerySpectrum(spectrum, precursorMz, null, selfSimilarity, 0d);
    }

    @NotNull
    @Override
    public Iterator<Peak> iterator() {
        return spectrum.iterator();
    }
}
