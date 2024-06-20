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
import de.unijena.bioinf.GibbsSampling.properties.*;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.middleware.model.compute.NullCheckMapBuilder;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * User/developer friendly parameter subset for the ZODIAC tool (Network base molecular formula re-ranking).
 * Needs results from Formula/SIRIUS Tool
 */
@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Zodiac extends Tool<ZodiacOptions> {

    /**
     * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds below 300 m/z.
     */
    @Schema(nullable = true)
    Integer consideredCandidatesAt300Mz;

    /**
     * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds above 800 m/z.
     */
    @Schema(nullable = true)
    Integer consideredCandidatesAt800Mz;

    /**
     * As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining.
     */
    @Schema(nullable = true)
    Boolean runInTwoSteps;

    /**
     * thresholdFilter = Defines the proportion of edges of the complete network which will be ignored.
     * minLocalConnections = Minimum number of compounds to which at least one candidate per compound must be connected to.
     */
    @Schema(nullable = true)
    ZodiacEdgeFilterThresholds edgeFilterThresholds;

    /**
     * iterations: "Number of epochs to run the Gibbs sampling. When multiple Markov chains are computed, all chains' iterations sum up to this value."
     * burnInPeriod: "Number of epochs considered as 'burn-in period'.
     * numberOfMarkovChains: Number of separate Gibbs sampling runs.
     */
    @Schema(nullable = true)
    ZodiacEpochs gibbsSamplerParameters;

    private Zodiac() {
        super(ZodiacOptions.class);
    }

    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return new NullCheckMapBuilder()
                .putIfNonNull("ZodiacNumberOfConsideredCandidatesAt300Mz", consideredCandidatesAt300Mz)
                .putIfNonNull("ZodiacNumberOfConsideredCandidatesAt800Mz", consideredCandidatesAt800Mz)
                .putIfNonNull("ZodiacRunInTwoSteps", runInTwoSteps)

                .putIfNonNullObj("ZodiacEpochs.iterations", gibbsSamplerParameters, it -> it.iterations)
                .putIfNonNullObj("ZodiacEpochs.burnInPeriod", gibbsSamplerParameters, it -> it.burnInPeriod)
                .putIfNonNullObj("ZodiacEpochs.numberOfMarkovChains", gibbsSamplerParameters, it -> it.numberOfMarkovChains)

                .putIfNonNullObj("ZodiacEdgeFilterThresholds.thresholdFilter", edgeFilterThresholds, it -> it.thresholdFilter)
                .putIfNonNullObj("ZodiacEdgeFilterThresholds.minLocalConnections", edgeFilterThresholds, it -> it.minLocalConnections)
                .putIfNonNullObj("ZodiacEdgeFilterThresholds.minLocalCandidates", edgeFilterThresholds, it -> it.minLocalCandidates)
                .toUnmodifiableMap();
    }

    public static Zodiac buildDefault(){
        return builderWithDefaults().build();
    }
    public static Zodiac.ZodiacBuilder<?,?> builderWithDefaults(){
        return Zodiac.builder()
                .enabled(false)
                .consideredCandidatesAt300Mz(PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacNumberOfConsideredCandidatesAt300Mz.class).value)
                .consideredCandidatesAt800Mz(PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacNumberOfConsideredCandidatesAt800Mz.class).value)
                .runInTwoSteps(PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacRunInTwoSteps.class).value)
                .edgeFilterThresholds(PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacEdgeFilterThresholds.class))
                .gibbsSamplerParameters(PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacEpochs.class));
    }
}