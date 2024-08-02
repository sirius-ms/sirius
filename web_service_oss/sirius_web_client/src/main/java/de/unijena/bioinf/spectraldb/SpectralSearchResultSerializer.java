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
import de.unijena.bioinf.projectspace.*;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Deprecated
public class SpectralSearchResultSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, SpectralSearchResult> {

    @Override
    public @Nullable SpectralSearchResult read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        String relPath = SpectralSearchLocations.SEARCH_RESULTS.relFilePath(id);
        String relPathParams = SpectralSearchLocations.SEARCH_PARAMS.relFilePath(id);

        if (!reader.exists(relPath) || !reader.exists(relPathParams))
            return null;

        AtomicReference<Deviation> precursorDev = new AtomicReference<>();
        AtomicReference<Deviation> peakDev = new AtomicReference<>();
        AtomicReference<SpectralMatchingType> alignmentType = new AtomicReference<>();
        AtomicInteger numOfResults = new AtomicInteger(0);

        reader.table(relPathParams, true, 0, 1, (row) -> {
            precursorDev.set(Deviation.fromString(row[0]));
            peakDev.set(Deviation.fromString(row[1]));
            alignmentType.set(SpectralMatchingType.valueOf(row[2]));
            numOfResults.set(Integer.parseInt(row[3]));
        });

        List<SpectralSearchResult.SearchResult> results = new ArrayList<>();

        reader.table(relPath, true, 0, numOfResults.get(), (row) -> results.add(
                SpectralSearchResult.SearchResult.builder()
                        .rank(Integer.parseInt(row[0]))
                        .querySpectrumIndex(Integer.parseInt(row[1]))
                        .dbName(row[2])
                        .dbId(row[3])
                        .uuid(Long.parseLong(row[4]))
                        .splash(row[5])
                        .candidateInChiKey(row[6])
                        .smiles(row[7])
                        .similarity(new SpectralSimilarity(Double.parseDouble(row[8]), Integer.parseInt(row[9])))
                        .exactMass(Double.parseDouble(row[10]))
                        .molecularFormula(MolecularFormula.parseOrThrow(row[11]))
                        .adduct(PrecursorIonType.fromString(row[12]))
                        .build()
        ));

        return SpectralSearchResult.builder()
                .precursorDeviation(precursorDev.get())
                .peakDeviation(peakDev.get())
                .alignmentType(alignmentType.get())
                .results(results)
                .build();
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<SpectralSearchResult> component) throws IOException {
        final SpectralSearchResult searchResult = component.orElseThrow(() -> new IllegalArgumentException("Could not find SpectralSearchResult to write for ID: " + id));

        writer.table(SpectralSearchLocations.SEARCH_PARAMS.relFilePath(id), new String[]{
                "precursorDeviation", "peakDeviation", "alignmentType", "numOfResults"
        }, List.of(new String[][]{{
            searchResult.getPrecursorDeviation().toString(),
            searchResult.getPeakDeviation().toString(),
            searchResult.getAlignmentType().toString(),
            Integer.toString(searchResult.getResults().size())
        }}));

        final String[] header = new String[]{
                "rank", "querySpectrumIndex", "dbName", "dbId", "uuid", "splash", "candidateInChiKey", "smiles", "similarity", "sharedPeaks", "exactMass", "molecularFormula", "adduct"
        };
        final String[] row = new String[header.length];
        writer.table(SpectralSearchLocations.SEARCH_RESULTS.relFilePath(id), header, searchResult.getResults().stream().map((hit) -> {
            row[0] = Integer.toString(hit.getRank());
            row[1] = Integer.toString(hit.getQuerySpectrumIndex());
            row[2] = hit.getDbName();
            row[3] = hit.getDbId();
            row[4] = String.valueOf(hit.getUuid());
            row[5] = hit.getSplash();
            row[6] = hit.getCandidateInChiKey();
            row[7] = hit.getSmiles();
            row[8] = Double.toString(hit.getSimilarity().similarity);
            row[9] = Integer.toString(hit.getSimilarity().sharedPeaks);
            row[10] = Double.toString(hit.getExactMass()); //todo: just added this to the serialization. Do we want this? mass, formula and addcut
            row[11] = hit.getMolecularFormula().toString();
            row[12] = hit.getAdduct().toString();
            return row;
        })::iterator);
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.delete(SpectralSearchLocations.SEARCH_RESULTS.relFilePath(id));
        writer.delete(SpectralSearchLocations.SEARCH_PARAMS.relFilePath(id));
    }

}
