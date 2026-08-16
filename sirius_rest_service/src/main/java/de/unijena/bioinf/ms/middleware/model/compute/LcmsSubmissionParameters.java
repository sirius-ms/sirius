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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.DataSmoothing;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcmsSubmissionParameters {
    /**
     * Sample names for each input file to link imported results, e.g. QuantTable back to the input data.
     * If NULL or empty sample names will be derived from the input files.
     * <p>
     * The names are matched to the input files by index. Partial lists are allowed: a NULL entry and any
     * input file without a corresponding entry get their name derived from the input file. Surplus entries
     * that match no input file are ignored.
     * <p>
     * Names must neither be blank nor duplicated, otherwise the import is rejected.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    protected List<String> sampleNames = null;

    /**
     * Sample type for each input file to be used to compute fold changes between blank and sample runs
     * If NULL or empty no fold changes will be computed during preprocessing.
     * <p>
     * The types are matched to the input files by index. In contrast to sampleNames either all or no sample
     * types have to be given: if the number of types does not match the number of input files or if any type
     * is NULL or blank, the import is rejected.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    protected List<String> sampleTypes = null;

    /**
     * Specifies whether LC/MS runs should be aligned
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "true")
    protected boolean alignLCMSRuns = true;

    /**
     * Noise level under which all peaks are considered to be likely noise. A peak has to be at least 3x noise level
     * to be picked as feature. Peaks with MS/MS are still picked even though they might be below noise level.
     * If not specified, the noise intensity is detected automatically from the data. We recommend NOT specifying
     * this parameter, as the automated detection is usually sufficient.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "-1")
    protected double noiseIntensity = -1;

    /**
     * Maximal allowed mass deviation for peaks in ms1 to be considered as belonging to the same trace.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    protected Deviation traceMaxMassDeviation = null;

    /**
     * Maximal allowed mass deviation for aligning features. If not specified, this parameter is estimated from data.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.DeviationDeserializer.class)
    protected Deviation alignMaxMassDeviation = null;

    /**
     * Maximum allowed retention time error in seconds for aligning features. If not specified, this parameter is estimated from the data.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "-1")
    protected double alignMaxRetentionTimeDeviation = -1;

    /**
     * Specifies filter algorithm to suppress noise.
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "AUTO", hidden = true)
    protected DataSmoothing filter = DataSmoothing.AUTO;

    /**
     * Minimum ratio between peak height and noise intensity for detecting features. By default, this value is 3. Features with good MS/MS are always picked independent of their intensity. For picking very low intensity features we recommend a min-snr of 2, but this will increase runtime and storage requirements
     */
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "3", hidden = false)
    protected double minSNR = 3;

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

    /**
     * Checks that sample names and types can be matched to the given input files, see {@code sampleNames}
     * and {@code sampleTypes} for the respective rules.
     *
     * @param numberOfInputFiles number of input files these parameters are submitted with
     * @throws IllegalArgumentException if names or types cannot be applied to the input files
     */
    public void validate(int numberOfInputFiles) throws IllegalArgumentException {
        if (sampleNames != null && !sampleNames.isEmpty()) {
            Set<String> uniqueNames = new HashSet<>();
            for (String sampleName : sampleNames) {
                if (sampleName == null)
                    continue; //name is derived from the input file
                if (sampleName.isBlank())
                    throw new IllegalArgumentException("Sample names must not be blank. " +
                            "Use null to derive the name of a sample from its input file.");
                if (!uniqueNames.add(sampleName))
                    throw new IllegalArgumentException("Sample names must be unique but '" + sampleName + "' was given multiple times.");
            }
        }

        if (sampleTypes != null && !sampleTypes.isEmpty()) {
            if (sampleTypes.size() != numberOfInputFiles)
                throw new IllegalArgumentException("Either all or no sample types have to be given, but "
                        + sampleTypes.size() + " sample types were given for " + numberOfInputFiles + " input files.");
            if (sampleTypes.stream().anyMatch(Utils::isNullOrBlank))
                throw new IllegalArgumentException("Either all or no sample types have to be given, but at least one given sample type is null or blank.");
        }
    }

}
