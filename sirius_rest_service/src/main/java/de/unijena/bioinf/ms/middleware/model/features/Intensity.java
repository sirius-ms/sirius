/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2026 Bright Giant GmbH
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

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.projectspace.SearchableFieldDoc;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The MS1 intensity of a feature (aligned over runs), as peak height and as area under the chromatographic
 * peak, each in raw instrument counts and in percent of the strongest feature it is compared with.
 * <p>
 * The values in counts are the highest over the runs the feature was detected in, as in the quantification
 * table. Data imported without an LC-MS trace (peak lists, features imported through the API) has the height
 * of the precursor peak in its MS1 spectrum and no area.
 * <p>
 * The percentages compare the feature with the strongest feature its run has held - or, for a feature without
 * a run, with the strongest feature the project has held - and take the best run. The reference only ever
 * rises: after the strongest feature of a run is deleted, the remaining ones keep the percentage they had, so
 * none of them reaches 100.
 * <p>
 * Each value is null when it is unknown.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intensity {

    /**
     * Peak height: the highest apex intensity over the runs the feature was detected in, or the precursor peak
     * height for data without an LC-MS trace. In raw instrument counts.
     */
    @IndexField(sortable = true)
    @SearchableFieldDoc(significantSuffixLength = 2) // labelled "intensity.<name>": the name alone says too little
    @Schema(nullable = true)
    private Double apex;

    /**
     * Area under the chromatographic peak: the highest over the runs the feature was detected in. In raw
     * instrument counts. Null for data without an LC-MS trace.
     */
    @IndexField(sortable = true)
    @SearchableFieldDoc(significantSuffixLength = 2) // labelled "intensity.<name>": the name alone says too little
    @Schema(nullable = true)
    private Double auc;

    /**
     * Peak height in percent of the highest peak height its run has held, the best over its runs:
     * {@code intensity.apexPercent:[10 TO *]} keeps the features that reach 10 % of that in at least one run.
     * A feature without a run is compared with the highest peak height the project has held.
     */
    @IndexField(sortable = true)
    @SearchableFieldDoc(significantSuffixLength = 2) // labelled "intensity.<name>": the name alone says too little
    @Schema(nullable = true)
    private Double apexPercent;

    /**
     * Area under the peak in percent of the highest area its run has held, the best over its runs.
     * Null for data without an LC-MS trace.
     */
    @IndexField(sortable = true)
    @SearchableFieldDoc(significantSuffixLength = 2) // labelled "intensity.<name>": the name alone says too little
    @Schema(nullable = true)
    private Double aucPercent;
}
