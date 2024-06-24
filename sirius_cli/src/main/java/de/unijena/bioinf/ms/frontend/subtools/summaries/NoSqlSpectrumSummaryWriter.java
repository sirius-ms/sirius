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

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.sirius.SpectraMatch;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class NoSqlSpectrumSummaryWriter implements AutoCloseable {

    final static String DOUBLE_FORMAT = "%.3f";
    final static String LONG_FORMAT = "%d";
    final static String HEADER =
            "spectralMatchRank\t" +
            "querySpectrumIndex\t" +
            "querySpectrumScanNumber\t" +
            "querySpectrumMsLevel\t" +
            "querySpectrumCE\t" +
            "querySpectrumIonization\t" + //todo this is proably just the charge?
            "similarity\t" +
            "sharedPeaks\t" +
            "referenceMsLevel\t" +
            "referenceCE\t" +
            "referenceAdduct\t" +
            "referencePrecursorMz\t" +
            "referenceInstrument\t" +
            "referenceInChIKey\t" +
            "referenceSmiles\t" +
            "referenceSplash\t" +
            "referenceName\t" +
            "referenceDbName\t" +
            // metadata for mapping
            "ionMass\t" +
            "retentionTimeInSeconds\t" +
            "retentionTimeInMinutes\t" +
            "alignedFeatureId\t" +
            "providedFeatureId";

    final BufferedWriter w;

    public NoSqlSpectrumSummaryWriter(BufferedWriter w) {
        this.w = w;
    }

    public void writeHeader() throws IOException {
        w.write(HEADER);
        w.newLine();
    }

    public void writeSpectralMatch(AlignedFeatures f, SpectraMatch match, MutableMs2Spectrum query, Ms2ReferenceSpectrum reference) throws IOException {
        w.write(String.format(LONG_FORMAT, match.getRank()));
        writeSep();

        w.write(String.format(LONG_FORMAT, match.getQuerySpectrumIndex()));
        writeSep();

        if (query.getScanNumber() > -1)
            w.write(String.format(LONG_FORMAT, query.getScanNumber()));
        writeSep();

        w.write(String.format(LONG_FORMAT, query.getMsLevel()));
        writeSep();

        if (query.getCollisionEnergy() != null)
            w.write(query.getCollisionEnergy().toString());
        writeSep();

        if (query.getIonization() != null)
            w.write(query.getIonization().toString()); //todo this is proably just the charge?
        writeSep();

        w.write(String.format(DOUBLE_FORMAT, 100 * match.getSimilarity().similarity));
        writeSep();

        w.write(String.format(LONG_FORMAT, match.getSimilarity().sharedPeaks));
        writeSep();

        w.write(String.format(LONG_FORMAT, reference.getMsLevel()));
        writeSep();

        if (reference.getCollisionEnergy() != null)
            w.write(reference.getCollisionEnergy().toString());
        writeSep();

        if (reference.getPrecursorIonType() != null)
            w.write(reference.getPrecursorIonType().toString());
        writeSep();

        w.write(String.format(DOUBLE_FORMAT, reference.getPrecursorMz()));
        writeSep();

        if (reference.getInstrumentation() != null)
            w.write(reference.getInstrumentation().description());
        writeSep();

        w.write(reference.getCandidateInChiKey());
        writeSep();

        w.write(reference.getSmiles());
        writeSep();

        w.write(reference.getSplash());
        writeSep();

        w.write(reference.getName());
        writeSep();

        w.write(match.getDbName());
        writeSep();

        w.write(String.format(DOUBLE_FORMAT, f.getAverageMass()));
        writeSep();
        w.write(Optional.ofNullable(f.getRetentionTime()).map(rt -> String.format("%.0f", rt.getMiddleTime())).orElse(""));
        writeSep();
        w.write(Optional.ofNullable(f.getRetentionTime()).map(rt -> String.format("%.2f", rt.getMiddleTime() / 60d)).orElse(""));
        writeSep();
        w.write(String.format(LONG_FORMAT, match.getAlignedFeatureId()));
        writeSep();
        w.write(Objects.requireNonNullElse(f.getExternalFeatureId(), String.format(LONG_FORMAT, match.getAlignedFeatureId())));
        w.newLine();

    }

    private void writeSep() throws IOException {
        w.write('\t');
    }

    @Override
    public void close() throws Exception {
        w.close();
    }

}
