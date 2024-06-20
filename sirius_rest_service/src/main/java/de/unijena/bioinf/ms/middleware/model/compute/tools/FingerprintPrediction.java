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

package de.unijena.bioinf.ms.middleware.model.compute.tools;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.unijena.bioinf.fingerid.annotations.FormulaResultThreshold;
import de.unijena.bioinf.ms.frontend.subtools.fingerprint.FingerprintOptions;
import de.unijena.bioinf.ms.middleware.model.compute.NullCheckMapBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.spectraldb.InjectSpectralLibraryMatchFormulas;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * User/developer friendly parameter subset for the CSI:FingerID Fingerprint tool
 * Needs results from Formula/SIRIUS Tool
 */

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FingerprintPrediction extends Tool<FingerprintOptions> {
    /**
     * If true, an adaptive soft threshold will be applied to only compute Fingerprints for promising formula candidates
     * Enabling is highly recommended.
     */
    @Schema(nullable = true)
    Boolean useScoreThreshold;

    /**
     * If true Fingerprint/Classes/Structures will be predicted for formulas candidates with
     * reference spectrum similarity > Sirius.minReferenceMatchScoreToInject will be predicted no matter which
     * score threshold rules apply.
     * If NULL default value will be used.
     */
    @Schema(nullable = true)
    Boolean alwaysPredictHighRefMatches;

    private FingerprintPrediction() {
        super(FingerprintOptions.class);
    }

    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putIfNonNull("FormulaResultThreshold", useScoreThreshold)
                .putIfNonNull("InjectSpectralLibraryMatchFormulas.alwaysPredict", alwaysPredictHighRefMatches)
                .toUnmodifiableMap();
    }

    public static FingerprintPrediction buildDefault(){
        return builderWithDefaults().build();
    }
    public static FingerprintPrediction.FingerprintPredictionBuilder<?,?> builderWithDefaults(){
        return FingerprintPrediction.builder()
                .enabled(true)
                .useScoreThreshold(PropertyManager.DEFAULTS.createInstanceWithDefaults(FormulaResultThreshold.class).useThreshold())
                .alwaysPredictHighRefMatches(PropertyManager.DEFAULTS.createInstanceWithDefaults(InjectSpectralLibraryMatchFormulas.class).isAlwaysPredict());
    }
}
