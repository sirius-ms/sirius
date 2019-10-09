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
