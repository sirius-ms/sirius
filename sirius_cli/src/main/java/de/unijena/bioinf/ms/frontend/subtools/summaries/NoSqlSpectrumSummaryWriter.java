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

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.SpectraMatch;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;

import java.io.IOException;
import java.util.*;

public class NoSqlSpectrumSummaryWriter extends SummaryTable {

    final static List<String> HEADER = List.of(
            "spectralMatchRank",
            "querySpectrumIndex",
            "querySpectrumScanNumber",
            "querySpectrumMsLevel",
            "querySpectrumCE",
            "similarity",
            "sharedPeaks",
            "referenceMsLevel",
            "referenceCE",
            "referenceAdduct",
            "referencePrecursorMz",
            "referenceInstrument",
            "referenceSmiles",
            "referenceSplash",
            "referenceName", //compound name
            "referenceLinks", //for user's custom DBs this is only one database+ID. However, from server-side we may provide "merged" libraries at some point, since GNPS, MoNa an co. share spectra.
            "InChIkey2D",
            // metadata for mapping
            "ionMass",
            "retentionTimeInSeconds",
            "retentionTimeInMinutes",
            "alignedFeatureId",
            "providedFeatureId",
            "overallFeatureQuality");

    public NoSqlSpectrumSummaryWriter(SummaryTableWriter writer) {
        super(writer);
    }

    public void writeHeader() throws IOException {
        writer.writeHeader(HEADER);
    }

    public void writeSpectralMatch(AlignedFeatures f, SpectraMatch match, MutableMs2Spectrum query, Ms2ReferenceSpectrum reference) throws IOException {
        List<Object> row = new ArrayList<>();

        row.add(match.getRank());

        row.add(match.getQuerySpectrumIndex());

        row.add(query.getScanNumber() > -1 ? query.getScanNumber() : null);

        row.add(query.getMsLevel());

        row.add(Optional.ofNullable(query.getCollisionEnergy()).map(CollisionEnergy::toString).orElse(null));

        row.add(match.getSimilarity().similarity); //max cosine is 1.0. In the GUI we show percent. Here it is just the number.

        row.add(match.getSimilarity().sharedPeaks);

        if (reference == null)
            row.add("N/A");
        else
            row.add(reference.getMsLevel());

        if (reference == null)
            row.add("N/A");
        else if (reference.getCollisionEnergy() == null)
            if (reference.getCe() == null)
                row.add("N/A");
            else
                row.add(reference.getCe());
        else
            row.add(reference.getCollisionEnergy().toString());

        if (reference == null || reference.getPrecursorIonType() == null)
            row.add("N/A");
        else
            row.add(reference.getPrecursorIonType().toString());

        if (reference == null)
            row.add("N/A");
        else
            row.add(reference.getPrecursorMz());

        if (reference == null || reference.getInstrumentation() == null)
            row.add("N/A");
        else
            row.add(reference.getInstrumentation().description());

        if (reference == null) {
            row.add(match.getSmiles());

            row.add("N/A");

            row.add("N/A");

            row.add(match.getDbName());

            row.add("N/A");
        } else {
            row.add(reference.getSmiles());

            row.add(reference.getSplash());

            row.add(reference.getName());

            Map<String, List<String>> dbMap = new HashMap<>();
            dbMap.computeIfAbsent(reference.getLibraryName(), k -> new ArrayList<>()).add(reference.getLibraryId());
            row.add(NoSqlStructureSummaryWriter.links(dbMap)); //this ensures same output format as for 'links' in NoSqlStructureSummaryWriter. However, currently, there is only 1 link.

            row.add(reference.getCandidateInChiKey());
        }

        row.add(f.getAverageMass());
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> Math.round(rt.getMiddleTime())).orElse(null));
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> rt.getMiddleTime() / 60d).orElse(null));
        row.add(String.valueOf(match.getAlignedFeatureId()));
        row.add(getMappingIdOrFallback(f));
        row.add(f.getDataQuality());

        writer.writeRow(row);
    }
}
