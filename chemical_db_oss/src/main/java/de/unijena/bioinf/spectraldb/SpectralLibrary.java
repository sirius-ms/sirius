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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceFragmentationTree;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.SearchPreparedSpectrum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface SpectralLibrary {
    FastCosine FAST_COSINE = new FastCosine();

    default FastCosine getFastCosine() {
        return FAST_COSINE;
    }

    long countAllSpectra() throws IOException;

    /*
    SPECTRA METHODS
     */

    default Iterable<Ms2ReferenceSpectrum> lookupSpectra(double precursorMz, Deviation deviation) throws ChemicalDatabaseException {
        return lookupSpectra(precursorMz, deviation, false);
    }

    Iterable<Ms2ReferenceSpectrum> lookupSpectra(double precursorMz, Deviation deviation, boolean withData) throws ChemicalDatabaseException;

    default Iterable<Ms2ReferenceSpectrum> lookupSpectra(String candidateInChiKey) throws ChemicalDatabaseException {
        return lookupSpectra(candidateInChiKey, false);
    }

    Iterable<Ms2ReferenceSpectrum> lookupSpectra(String candidateInChiKey, boolean withData) throws ChemicalDatabaseException;

    default Iterable<Ms2ReferenceSpectrum> lookupSpectra(MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupSpectra(formula, false);
    }

    Iterable<Ms2ReferenceSpectrum> lookupSpectra(MolecularFormula formula, boolean withData) throws ChemicalDatabaseException;

    /*
    Cosine Similarity Search

     */

    Stream<LibraryHit> queryAgainstLibraryWithPrecursorMass(double precursorMz, int chargeAndPolarity, SpectralLibrarySearchSettings settings, List<SearchPreparedSpectrum> query) throws IOException;
    Stream<LibraryHit> queryAgainstLibrary(int chargeAndPolarity, SpectralLibrarySearchSettings settings, List<SearchPreparedSpectrum> query) throws IOException;

    Stream<LibraryHit> queryAgainstLibraryByMergedReference(List<MergedReferenceSpectrum> mergedRefQueries, SpectralLibrarySearchSettings settings, @NotNull List<SearchPreparedSpectrum> query, @Nullable SearchPreparedSpectrum mergedQuery) throws IOException;
    Stream<LibraryHit> queryAgainstLibraryByMergedReference(MergedReferenceSpectrum mergedRefQuery, SpectralLibrarySearchSettings settings, @NotNull List<SearchPreparedSpectrum> query, @Nullable SearchPreparedSpectrum mergedQuery) throws IOException;

    /*
    Other methods
     */

    default Stream<MergedReferenceSpectrum> getMergedReferenceSpectra(double precursorMz, int chargeAndPolarity, Deviation precursorDeviation) throws ChemicalDatabaseException {
        return getMergedReferenceSpectra(precursorMz, chargeAndPolarity, precursorDeviation, true);
    }
    Stream<MergedReferenceSpectrum> getMergedReferenceSpectra(double precursorMz, int chargeAndPolarity, Deviation precursorDeviation, boolean withSpectrum) throws ChemicalDatabaseException;

    default MergedReferenceSpectrum getMergedReferenceSpectrum(String candidateInChiKey, PrecursorIonType precursorIonType) throws ChemicalDatabaseException {
        return getMergedReferenceSpectrum(candidateInChiKey, precursorIonType, true);
    }
    MergedReferenceSpectrum getMergedReferenceSpectrum(String candidateInChiKey, PrecursorIonType precursorIonType, boolean withSpectrum) throws ChemicalDatabaseException;

    ReferenceFragmentationTree getReferenceTree(long uuid) throws ChemicalDatabaseException;

    Ms2ReferenceSpectrum getReferenceSpectrum(long uuid) throws ChemicalDatabaseException;
    ReferenceSpectrum getReferenceSpectrum(long uuid, SpectrumType spectrumType) throws ChemicalDatabaseException;

    Ms2ReferenceSpectrum queryAgainstIndividualSpectrum(long uuid) throws ChemicalDatabaseException;

    Iterable<Ms2ReferenceSpectrum> fetchSpectralData(Iterable<Ms2ReferenceSpectrum> references) throws ChemicalDatabaseException;

    Ms2ReferenceSpectrum fetchSpectralData(Ms2ReferenceSpectrum reference) throws ChemicalDatabaseException;

    void forEachSpectrum(Consumer<Ms2ReferenceSpectrum> consumer) throws IOException;

    void forEachSpectrum(Consumer<Ms2ReferenceSpectrum> consumer, boolean withData) throws IOException;
    void forEachMergedSpectrum(Consumer<MergedReferenceSpectrum> consumer) throws IOException;

}
