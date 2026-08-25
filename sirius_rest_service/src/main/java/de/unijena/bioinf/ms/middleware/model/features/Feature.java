/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.middleware.model.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * A feature as it has been detected in one single run, in contrast to an AlignedFeature which combines the
 * features of the same compound over all runs it was detected in.
 * <p>
 * It provides the properties that are specific to the run it was detected in, such as its position on the
 * retention time axis of that run and its quantity within that run.
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Feature {

    /**
     * Identifier
     */
    @NotNull
    protected String featureId;

    /**
     * ID of the AlignedFeature this feature belongs to
     */
    protected String alignedFeatureId;

    /**
     * ID of the run this feature belongs to
     */
    protected String runId;

    /**
     * Average m/z over the whole feature
     */
    @Schema(nullable = true)
    protected Double averageMz;

    /**
     * m/z at the apex of the feature, the m/z this feature was measured at in its run
     */
    @Schema(nullable = true)
    protected Double apexMz;

    /**
     * Start of the feature on the retention time axis in seconds
     */
    @Schema(nullable = true)
    protected Double rtStartSeconds;

    /**
     * End of the feature on the retention time axis in seconds
     */
    @Schema(nullable = true)
    protected Double rtEndSeconds;

    /**
     * Apex of the feature on the retention time axis in seconds
     */
    @Schema(nullable = true)
    protected Double rtApexSeconds;

    /**
     * Full width at half maximum of the feature on the retention time axis in seconds
     */
    @Schema(nullable = true)
    protected Double rtFwhmSeconds;

    /**
     * Feature quantity measured as the intensity of the apex of the feature
     */
    @Schema(nullable = true)
    protected Double apexIntensity;

    /**
     * Feature quantity measured as the area under the curve of the whole feature
     */
    @Schema(nullable = true)
    protected Double areaUnderCurve;

}
