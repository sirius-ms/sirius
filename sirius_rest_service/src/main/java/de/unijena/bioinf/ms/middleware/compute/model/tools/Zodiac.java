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

package de.unijena.bioinf.ms.middleware.compute.model.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.GibbsSampling.properties.*;
import de.unijena.bioinf.ms.frontend.subtools.zodiac.ZodiacOptions;
import de.unijena.bioinf.ms.properties.PropertyManager;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * User/developer friendly parameter subset for the ZODIAC tool (Network base molecular formula re-ranking).
 */
@Getter
@Setter
public class Zodiac extends Tool<ZodiacOptions> {

    /**
     * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds below 300 m/z.
     */
    ZodiacNumberOfConsideredCandidatesAt300Mz consideredCandidatesAt300Mz = PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacNumberOfConsideredCandidatesAt300Mz.class);
    /**
     * Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds above 800 m/z.
     */
    ZodiacNumberOfConsideredCandidatesAt800Mz consideredCandidatesAt800Mz = PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacNumberOfConsideredCandidatesAt800Mz.class);
    /**
     * As default ZODIAC runs a 2-step approach. First running 'good quality compounds' only, and afterwards including the remaining.
     */
    ZodiacRunInTwoSteps runInTwoSteps = PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacRunInTwoSteps.class);

    /**
     * thresholdFilter = Defines the proportion of edges of the complete network which will be ignored.
     * minLocalConnections = Minimum number of compounds to which at least one candidate per compound must be connected to.
     */
    ZodiacEdgeFilterThresholds edgeFilterThresholds = PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacEdgeFilterThresholds.class);

    /**
     * iterations: "Number of epochs to run the Gibbs sampling. When multiple Markov chains are computed, all chains' iterations sum up to this value."
     * burnInPeriod: "Number of epochs considered as 'burn-in period'.
     * numberOfMarkovChains: Number of separate Gibbs sampling runs.
     */
    ZodiacEpochs gibbsSamplerParameters = PropertyManager.DEFAULTS.createInstanceWithDefaults(ZodiacEpochs.class);

    public Zodiac() {
        super(ZodiacOptions.class);
    }

    @JsonIgnore
    @Override
    public Map<String, String> asConfigMap() {
        return Map.of(
                "ZodiacNumberOfConsideredCandidatesAt300Mz", String.valueOf(consideredCandidatesAt300Mz.value),
                "ZodiacNumberOfConsideredCandidatesAt800Mz", String.valueOf(consideredCandidatesAt800Mz.value),
                "ZodiacRunInTwoSteps", String.valueOf(runInTwoSteps.value),

                "ZodiacEpochs.iterations", String.valueOf(gibbsSamplerParameters.iterations),
                "ZodiacEpochs.burnInPeriod", String.valueOf(gibbsSamplerParameters.burnInPeriod),
                "ZodiacEpochs.numberOfMarkovChains", String.valueOf(gibbsSamplerParameters.numberOfMarkovChains),

                "ZodiacEdgeFilterThresholds.thresholdFilter", String.valueOf(edgeFilterThresholds.thresholdFilter),
                "ZodiacEdgeFilterThresholds.minLocalConnections", String.valueOf(edgeFilterThresholds.minLocalConnections),
                "ZodiacEdgeFilterThresholds.minLocalCandidates", String.valueOf(edgeFilterThresholds.minLocalCandidates)
        );
    }
}