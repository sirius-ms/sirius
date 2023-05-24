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
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.apache.commons.lang3.tuple.Pair;

public interface SpectralLibrary {

    <P extends Peak, A extends AbstractSpectralAlignment> Iterable<Pair<SpectralSimilarity, Ms2SpectralMetadata>> matchingSpectra(
            Ms2Spectrum<P> spectrum,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType
    ) throws ChemicalDatabaseException;

    <P extends Peak, A extends AbstractSpectralAlignment> Iterable<Pair<SpectralSimilarity, Ms2SpectralMetadata>> matchingSpectra(
            Ms2Spectrum<P> spectrum,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType,
            boolean parallel
    ) throws ChemicalDatabaseException;

    Iterable<Ms2SpectralData> lookupSpectra(double precursorMz, Deviation deviation) throws ChemicalDatabaseException;

    Iterable<Ms2SpectralMetadata> getMetaData(Iterable<Ms2SpectralData> data) throws ChemicalDatabaseException;

    Iterable<Ms2SpectralData> getSpectralData(Iterable<Ms2SpectralMetadata> metadata) throws ChemicalDatabaseException;

}
