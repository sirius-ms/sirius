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
    protected boolean alignLCMSRuns;

    /**
     * Features must be larger than <value> * detected noise level.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "2.0")
    protected double noise;

    /**
     * Features must have larger persistence (intensity above valley) than <value> * max trace intensity.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "0.1")
    protected double persistence;

    /**
     * Merge neighboring features with valley less than <value> * intensity.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "0.8")
    protected double merge;

    /**
     * Specifies filter algorithm to suppress noise.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "AUTO", enumAsRef = true)
    protected DataSmoothing filter;

    /**
     * Sigma (kernel width) for gaussian filter algorithm.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "3.0")
    protected double gaussianSigma;

    /**
     * Number of coefficients for wavelet filter algorithm.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "20")
    protected int waveletScale;

    /**
     * Wavelet window size (%) for wavelet filter algorithm.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "11")
    protected double waveletWindow;


}
