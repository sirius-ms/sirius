/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.summaries;

import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class DataQualitySummaryWriter extends SummaryTable {

    final static List<String> CATEGORIES = List.of(
            QualityReport.PEAK_QUALITY,
            QualityReport.ALIGNMENT_QUALITY,
            QualityReport.ISOTOPE_QUALITY,
            QualityReport.MS2_QUALITY,
            QualityReport.ADDUCT_QUALITY
    );

    final static List<String> SHARED_COLUMNS = List.of(
            "ionMass",
            "retentionTimeInSeconds",
            "retentionTimeInMinutes",
            "alignedFeatureId",
            "compoundId",
            "mappingFeatureId",
            "overallFeatureQuality");

    DataQualitySummaryWriter(SummaryTableWriter writer) {
        super(writer);
    }

    public void writeHeader() throws IOException {
        List<String> header = Stream.concat(SHARED_COLUMNS.stream(), CATEGORIES.stream()).toList();
        writer.writeHeader(header);
    }

    public void writeFeatureQuality(AlignedFeatures f, QualityReport qualityReport) throws IOException {
        List<Object> row = new ArrayList<>();

        addCommonColumns(row, f);
        addCategoryColumns(row, qualityReport);

        writer.writeRow(row);
    }

    private void addCommonColumns(List<Object> row, AlignedFeatures f) {
        row.add(f.getAverageMass());
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> Math.round(rt.getMiddleTime())).orElse(null));
        row.add(Optional.ofNullable(f.getRetentionTime()).map(rt -> rt.getMiddleTime() / 60d).orElse(null));

        row.add(String.valueOf(f.getAlignedFeatureId()));
        row.add(String.valueOf(f.getCompoundId()));
        row.add(getMappingIdOrFallback(f));
        row.add(f.getDataQuality());
    }

    private void addCategoryColumns(List<Object> row, QualityReport qualityReport) {
        if (qualityReport == null) {
            row.addAll(CATEGORIES.stream().map(c -> null).toList());
            return;
        }

        for (String cName : CATEGORIES) {
            QualityReport.Category c = qualityReport.getCategories().get(cName);
            row.add(c == null ? null : c.getOverallQuality());
        }
    }
}
