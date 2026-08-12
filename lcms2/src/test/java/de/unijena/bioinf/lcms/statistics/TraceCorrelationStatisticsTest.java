package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The distribution of correlations between two profiles that genuinely are one elution, learned from isotope pairs.
 */
public class TraceCorrelationStatisticsTest {

    private static final int LENGTH = 200;
    private static final double STEP = 0.2;
    private static final Deviation DEVIATION = new Deviation(8, 3e-4);
    private static final double NOISE = 100d;

    private LCMSStorageFactory factory;

    @AfterEach
    public void tearDown() {
        if (factory != null) factory.close();
    }

    private static ScanPointMapping axis() {
        final double[] rts = new double[LENGTH];
        final int[] ids = new int[LENGTH];
        for (int k = 0; k < LENGTH; ++k) {
            rts[k] = k * STEP;
            ids[k] = k;
        }
        return new ScanPointMapping(rts, ids, null);
    }

    /** a gaussian trace spanning the whole axis */
    private static ContiguousTrace trace(ScanPointMapping mapping, double mz, double centre, double apex) {
        final double[] masses = new double[LENGTH];
        final float[] intensities = new float[LENGTH];
        for (int k = 0; k < LENGTH; ++k) {
            masses[k] = mz;
            intensities[k] = (float) (apex * Math.exp(-0.5 * Math.pow((k * STEP - centre) / 3d, 2)));
        }
        return new ContiguousTrace(mapping, 0, LENGTH - 1, masses, intensities);
    }

    /** a sample holding the given traces, nothing else */
    private ProcessedSample sampleWith(ContiguousTrace... traces) throws IOException {
        factory = LCMSStorage.temporaryStorage(null, false);
        final LCMSStorage storage = factory.createNewStorage();
        final ScanPointMapping mapping = axis();
        final float[] noise = new float[LENGTH];
        Arrays.fill(noise, (float) NOISE);
        storage.setStatistics(SampleStats.builder().noiseLevelPerScan(noise).ms2NoiseLevel((float) NOISE)
                .minimumIntensity(1f).ms1MassDeviationWithinTraces(DEVIATION)
                .minimumMs1MassDeviationBetweenTraces(DEVIATION).build());
        for (ContiguousTrace trace : traces) storage.getTraceStorage().addContigousTrace(trace);
        storage.commit();
        return new ProcessedSample(mapping, storage, 1, 0);
    }

    @Test
    public void anIsotopePairIsFoundAndCorrelatesWell() throws IOException {
        final ScanPointMapping mapping = axis();
        final ProcessedSample sample = sampleWith(
                trace(mapping, 400.2, 20d, 1_000_000d),
                trace(mapping, 400.2 + TraceCorrelationStatistics.CARBON_ISOTOPE_DELTA, 20d, 110_000d));

        final TraceCorrelationStatistics learned =
                TraceCorrelationStatistics.learn(sample, DEVIATION, NOISE);

        assertFalse(learned.isEmpty(), "the pair has to be found");
        int found = 0;
        for (int bin = 0; bin < TraceCorrelationStatistics.binCount(); ++bin) {
            for (float correlation : learned.observations(bin)) {
                ++found;
                assertTrue(correlation > 0.99, "identical shapes correlate: " + correlation);
            }
        }
        // one observation at full resolution plus one per subsample
        assertTrue(found >= 3, "measured at several point counts, got " + found);
    }

    @Test
    public void aHeavierTraceThatIsMoreIntenseIsNotAnIsotope() throws IOException {
        // if the heavier partner is the stronger one, this is not a monoisotopic peak with its isotope
        final ScanPointMapping mapping = axis();
        final ProcessedSample sample = sampleWith(
                trace(mapping, 400.2, 20d, 100_000d),
                trace(mapping, 400.2 + TraceCorrelationStatistics.CARBON_ISOTOPE_DELTA, 20d, 1_000_000d));

        assertTrue(TraceCorrelationStatistics.learn(sample, DEVIATION, NOISE).isEmpty());
    }

    @Test
    public void anUnrelatedMassIsNoPair() throws IOException {
        final ScanPointMapping mapping = axis();
        final ProcessedSample sample = sampleWith(
                trace(mapping, 400.2, 20d, 1_000_000d),
                trace(mapping, 412.9, 20d, 100_000d));

        assertTrue(TraceCorrelationStatistics.learn(sample, DEVIATION, NOISE).isEmpty(),
                "twelve Dalton apart is not a carbon isotope");
    }

    @Test
    public void aNoisyPairLowersTheDistribution() throws IOException {
        final ScanPointMapping mapping = axis();
        final ContiguousTrace mono = trace(mapping, 400.2, 20d, 1_000_000d);
        // the isotope with noise on it, as a weak trace really has
        final ContiguousTrace clean = trace(mapping,
                400.2 + TraceCorrelationStatistics.CARBON_ISOTOPE_DELTA, 20d, 30_000d);
        final double[] masses = new double[LENGTH];
        final float[] intensities = new float[LENGTH];
        for (int k = 0; k < LENGTH; ++k) {
            masses[k] = clean.mz(k);
            intensities[k] = (float) Math.max(0d, clean.intensity(k) + ((k * 37) % 11 - 5) * 2_000d);
        }
        final ProcessedSample sample = sampleWith(mono,
                new ContiguousTrace(mapping, 0, LENGTH - 1, masses, intensities));

        final TraceCorrelationStatistics learned = TraceCorrelationStatistics.learn(sample, DEVIATION, NOISE);
        assertFalse(learned.isEmpty());
        // one pair is far too little to make a bin usable, so the observations themselves are what to look at
        double lowest = 1d, highest = -1d;
        for (int bin = 0; bin < TraceCorrelationStatistics.binCount(); ++bin) {
            for (float correlation : learned.observations(bin)) {
                lowest = Math.min(lowest, correlation);
                highest = Math.max(highest, correlation);
            }
        }
        assertTrue(highest < 0.999, "noise has to show: " + highest);
        assertTrue(lowest > 0.5, "but it is still the same elution: " + lowest);
    }

    /** a bin of its own is only trusted when it holds enough pairs; otherwise it joins the one below it */
    @Test
    public void aThinBinIsMergedWithTheOneBelowIt() {
        final float[][] bins = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(bins, new float[0]);
        final int strong = TraceCorrelationStatistics.binOf(20, 1000 * NOISE, NOISE);
        final int weaker = TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE);
        assertNotEquals(strong, weaker, "precondition: two different bins");
        bins[strong] = filled(10, 0.99f);                                        // too thin to stand alone
        bins[weaker] = filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, 0.80f);
        final TraceCorrelationStatistics statistics = new TraceCorrelationStatistics(bins);

        // the thin strong bin is answered by the group it was merged into, which is dominated by the weaker bin
        assertEquals(0.80, statistics.quantileOfSameElution(20, 1000 * NOISE, NOISE, 0.05), 1e-6);
    }

    /** more signal cannot make a co-elution harder to see, so usability has to be monotone in intensity */
    @Test
    public void aStrongBinIsNeverUnusableWhileAWeakerOneIsUsable() {
        final float[][] bins = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(bins, new float[0]);
        final int weaker = TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE);
        final int strong = TraceCorrelationStatistics.binOf(20, 1000 * NOISE, NOISE);
        // enough pairs to stand alone, but a distribution that would be discarded - noise in the estimate
        bins[weaker] = filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 4, 0.95f);
        bins[strong] = filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, 0.10f);
        final TraceCorrelationStatistics statistics = new TraceCorrelationStatistics(bins);

        final double answer = statistics.quantileOfSameElution(20, 1000 * NOISE, NOISE, 0.05);
        assertFalse(Double.isNaN(answer), "the stronger bin must not end up unusable on its own");
        assertEquals(0.95, answer, 1e-6,
                "it is answered by the usable segment below it, whose distribution stays intact");
        // and the minimum usable intensity stays where the weaker, usable bin is
        assertTrue(statistics.minimumUsableIntensity(20) <= 20d,
                "got " + statistics.minimumUsableIntensity(20));
    }

    /** the regime near the noise cannot be judged, and saying so is not the same as reporting a low correlation */
    @Test
    public void aBinThatCannotBeatTheMinimumIsDiscarded() {
        final float[][] bins = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(bins, new float[0]);
        final int weak = TraceCorrelationStatistics.binOf(20, 1.5 * NOISE, NOISE);
        bins[weak] = filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, 0.2f);
        final TraceCorrelationStatistics statistics = new TraceCorrelationStatistics(bins);

        assertTrue(Double.isNaN(statistics.quantileOfSameElution(20, 1.5 * NOISE, NOISE, 0.05)),
                "a regime where a known co-elution does not correlate cannot yield a cutoff");
        assertTrue(Double.isNaN(statistics.minimumUsableIntensity(20)), "and nothing is usable here");
    }

    private static float[] filled(int count, float value) {
        final float[] values = new float[count];
        Arrays.fill(values, value);
        return values;
    }

    @Test
    public void nothingLearnedMeansNoAnswerRatherThanAGuess() {
        final TraceCorrelationStatistics empty = TraceCorrelationStatistics.empty();
        assertTrue(empty.isEmpty());
        assertTrue(Double.isNaN(empty.quantileOfSameElution(5, 1000d, NOISE, 0.05)),
                "an unlearned statistic must not invent a cutoff");
    }

    @Test
    public void theConditionsSeparateWhatTheyClaimTo() {
        // few against many points
        assertNotEquals(TraceCorrelationStatistics.binOf(4, 1e6, NOISE),
                TraceCorrelationStatistics.binOf(20, 1e6, NOISE));
        assertEquals(TraceCorrelationStatistics.binOf(3, 1e6, NOISE),
                TraceCorrelationStatistics.binOf(5, 1e6, NOISE), "both below the split");
        assertEquals(TraceCorrelationStatistics.binOf(6, 1e6, NOISE),
                TraceCorrelationStatistics.binOf(50, 1e6, NOISE), "both above it");
        // and the intensity decades
        assertNotEquals(TraceCorrelationStatistics.binOf(20, 5 * NOISE, NOISE),
                TraceCorrelationStatistics.binOf(20, 5000 * NOISE, NOISE));
        // a trace with no noise level to compare against is treated as the weakest, never the strongest
        assertEquals(TraceCorrelationStatistics.binOf(20, 1e6, 0d),
                TraceCorrelationStatistics.binOf(20, 0.5 * NOISE, NOISE),
                "without a noise level there is nothing to claim");
        // the intensity bins are ordered, so a stronger trace never lands in a lower bin
        int previous = -1;
        for (double multiple : new double[]{0.5, 1.5, 3, 7, 15, 30, 70, 500}) {
            final int bin = TraceCorrelationStatistics.binOf(20, multiple * NOISE, NOISE);
            assertTrue(bin > previous, "bins have to increase with intensity, at " + multiple + "x");
            previous = bin;
        }
    }
}
