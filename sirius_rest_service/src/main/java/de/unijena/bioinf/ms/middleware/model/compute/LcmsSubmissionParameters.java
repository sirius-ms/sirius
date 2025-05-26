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

package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.DataSmoothing;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcmsSubmissionParameters {
    /**
     * Specifies whether LC/MS runs should be aligned
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "true")
    protected boolean alignLCMSRuns = true;

    /**
     * Noise level under which all peaks are considered to be likely noise. A peak has to be at least 3x noise level
     * to be picked as feature. Peaks with MS/MS are still picked even though they might be below noise level.
     * If not specified, the noise intensity is detected automatically from data. We recommend to NOT specify
     * this parameter, as the autmated detection is usually sufficient.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "-1")
    protected double noiseIntensity = -1;

    /**
     * Maximal allowed mass deviation for peaks in ms1 to be considered as belonging to the same trace.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "null")
    protected Deviation ms1MassDeviation = null;

    /**
     * Maximal allowed mass deviation for aligning features. If not specified, this parameter is estimated from data.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "null")
    protected Deviation alignMassDeviation = null;

    /**
     * Maximal allowed retention time error in seconds for aligning features. If not specified, this parameter is estimated from data.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "-1")
    protected double alignRetentionTimeError = -1;

    /**
     * Specifies filter algorithm to suppress noise.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "AUTO", hidden = true)
    protected DataSmoothing filter = DataSmoothing.AUTO;

    /**
     * Sigma (kernel width) for gaussian filter algorithm.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "0.5", hidden = true)
    protected double gaussianSigma = 0.5;

    /**
     * Number of coefficients for wavelet filter algorithm.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "8", hidden = true)
    protected int waveletScale = 8;


}
