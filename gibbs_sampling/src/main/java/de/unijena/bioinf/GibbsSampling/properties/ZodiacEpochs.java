/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.GibbsSampling.properties;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

public class ZodiacEpochs implements Ms2ExperimentAnnotation {

    /**
     * Number of epochs to run the Gibbs sampling. When multiple Markov chains are computed, all chains' iterations sum up to this value.
     */
    @DefaultProperty public final int iterations;

    /**
     * Number of epochs considered as 'burn-in period'.
     * Samples from the beginning of a Markov chain do not accurately represent the desired distribution of candidates and are not used to estimate the ZODIAC score.
     */
    @DefaultProperty public final int burnInPeriod;

    /**
     * Number of separate Gibbs sampling runs.
     */
    @DefaultProperty public final int numberOfMarkovChains;


    private ZodiacEpochs() {
        iterations = -1;
        burnInPeriod = -1;
        numberOfMarkovChains = -1;
    }

    public ZodiacEpochs(int iterations, int burnInPeriod, int numberOfMarkovChains) {
        this.iterations = iterations;
        this.burnInPeriod = burnInPeriod;
        this.numberOfMarkovChains = numberOfMarkovChains;
    }
}
