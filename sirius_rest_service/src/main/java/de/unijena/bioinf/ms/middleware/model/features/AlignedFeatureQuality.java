/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlignedFeatureQuality {
    @Schema(enumAsRef = true, name = "AlignedFeatureQualityOptField", nullable = true)
    public enum OptField {none, qualityFlags, lcmsFeatureQuality}

    protected String alignedFeatureId;

    // Data and Result quality
    /**
     * Contains all pre-computation quality information that belong to
     * this feature (aligned over runs), such as information about the quality of the peak shape, MS2 spectrum etc.,
     * see ({@link CompoundQuality.CompoundQualityFlag})
     * <p>
     * Each Feature has a Set of Quality assessment flags.
     */
    @Schema(nullable = true)
    protected EnumSet<CompoundQuality.CompoundQualityFlag> qualityFlags;

    /**
     * LCMS feature-based quality information as also provided in the LCMS-view in the GUI
     */
    @Schema(nullable = true)
    protected LCMSFeatureQuality lcmsFeatureQuality;
}
