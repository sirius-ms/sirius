/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public interface SpectralLibrary {

    default <P extends Peak, A extends AbstractSpectralAlignment> Iterable<SearchResult> matchingSpectra(
            Iterable<Ms2Spectrum<P>> queries,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation
    ) throws ChemicalDatabaseException {
        return matchingSpectra(queries, precursorMzDeviation, maxPeakDeviation, IntensityWeightedSpectralAlignment.class);
    }

    <P extends Peak, A extends AbstractSpectralAlignment> Iterable<SearchResult> matchingSpectra(
            Iterable<Ms2Spectrum<P>> queries,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType
    ) throws ChemicalDatabaseException;

    default Iterable<Ms2ReferenceSpectrum> lookupSpectra(double precursorMz, Deviation deviation) throws ChemicalDatabaseException {
        return lookupSpectra(precursorMz, deviation, false);
    }

    Iterable<Ms2ReferenceSpectrum> lookupSpectra(double precursorMz, Deviation deviation, boolean withData) throws ChemicalDatabaseException;

    default Iterable<Ms2ReferenceSpectrum> lookupSpectra(String inchiKey2d) throws ChemicalDatabaseException {
        return lookupSpectra(inchiKey2d, false);
    }

    Iterable<Ms2ReferenceSpectrum> lookupSpectra(String inchiKey2d, boolean withData) throws ChemicalDatabaseException;

    Iterable<Ms2ReferenceSpectrum> getSpectralData(Iterable<Ms2ReferenceSpectrum> references) throws ChemicalDatabaseException;

    Ms2ReferenceSpectrum getSpectralData(Ms2ReferenceSpectrum reference) throws ChemicalDatabaseException;

    @Builder
    @Getter
    final class SearchResult {

        private Ms2Spectrum<? extends Peak> query;

        @Builder.Default
        @Setter
        private int rank = -1;

        private SpectralSimilarity similarity;

        private Ms2ReferenceSpectrum reference;

    }


}
