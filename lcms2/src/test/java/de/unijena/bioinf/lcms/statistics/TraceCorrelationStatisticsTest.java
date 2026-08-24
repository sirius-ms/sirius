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
import java.util.List;

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

    /**
     * The strong tail keeps its own company rather than being dissolved into a weak bin that contradicts it.
     * <p>
     * The bin counts are those a real sample produced, where the pairs above ten times the noise number in the tens
     * while the bin just above the noise holds hundreds. The tail correlates as a known co-elution should and the
     * weak bin does not, so pooling the two answers the strong regime with the weak bin's distribution - and since
     * that distribution is below the minimum, the strong regime comes out unjudgeable although it was measured
     * perfectly well. The accumulation of thin bins is what prevents this, but only while the floor is low enough
     * that the tail can reach it before it has to swallow the weak bin.
     */
    @Test
    public void theStrongTailIsNotDissolvedIntoAContradictingWeakBin() {
        final float[][] bins = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(bins, new float[0]);
        // 2-5x holds the many mediocre pairs, and every bin above it the few good ones
        bins[TraceCorrelationStatistics.binOf(20, 3 * NOISE, NOISE)] = filled(234, 0.2f);
        bins[TraceCorrelationStatistics.binOf(20, 7 * NOISE, NOISE)] = filled(88, 0.9f);
        bins[TraceCorrelationStatistics.binOf(20, 15 * NOISE, NOISE)] = filled(27, 0.99f);
        bins[TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE)] = filled(21, 0.99f);
        bins[TraceCorrelationStatistics.binOf(20, 70 * NOISE, NOISE)] = filled(9, 0.99f);
        bins[TraceCorrelationStatistics.binOf(20, 500 * NOISE, NOISE)] = filled(2, 0.99f);
        final TraceCorrelationStatistics statistics = new TraceCorrelationStatistics(bins);

        final double strong = statistics.quantileOfSameElution(20, 30 * NOISE, NOISE, 0.05);
        assertFalse(Double.isNaN(strong),
                "the tail holds 147 pairs that agree; it must not be answered by the 234 that do not");
        assertEquals(0.9, strong, 1e-6, "the cutoff is the tail's own quantile, not the weak bin's");
        // and the weak bin still reports that it cannot be judged, which is the honest answer there
        assertTrue(Double.isNaN(statistics.quantileOfSameElution(20, 3 * NOISE, NOISE, 0.05)));
        assertEquals(5d, statistics.minimumUsableIntensity(20), 1e-6,
                "co-elution becomes judgeable where the tail begins");
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

    /**
     * What a caller is told when the sample could not learn a cutoff, and what a learned one may not undercut.
     * <p>
     * Two separate claims. Where nothing was learned there still has to be a number, and it belongs to the datapoint
     * condition rather than being one constant for both: a correlation over four points and one over forty are not
     * the same measurement. And a learned cutoff may only ever make the bar stricter - the isotope distribution is a
     * lower bound on what a genuine co-elution reaches, so following it downwards would group on evidence that the
     * few-point regime cannot actually supply, and grouping two distinct compounds loses both of them while failing
     * to group them only forgoes the pollution accounting.
     */
    @Test
    public void aCutoffIsNeverLowerThanTheConditionsOwnMinimum() {
        final double few = TraceCorrelationStatistics.minimumCutoff(4);
        final double many = TraceCorrelationStatistics.minimumCutoff(20);
        assertTrue(few <= many, "the few-point condition is the noisier measurement and may not ask for more");

        // where nothing was measured this statistic answers with its own floor and says that it was a fallback; what
        // a consumer does about that is the consumer's policy and is not decided here
        final TraceCorrelationStatistics nothing = TraceCorrelationStatistics.empty();
        assertEquals(few, nothing.cutoffFor(4, 1000 * NOISE, NOISE, 0.05), 1e-9);
        assertEquals(many, nothing.cutoffFor(20, 1000 * NOISE, NOISE, 0.05), 1e-9);
        assertFalse(nothing.hasLearnedCutoffFor(4, 1000 * NOISE, NOISE), "and it says so");
        assertFalse(nothing.hasLearnedCutoffFor(20, 1000 * NOISE, NOISE));

        // a learned distribution that is more lenient than the minimum does not lower it
        final float[][] lenient = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(lenient, new float[0]);
        lenient[TraceCorrelationStatistics.binOf(4, 30 * NOISE, NOISE)] =
                filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, 0.6f);   // below any allowed minimum
        final TraceCorrelationStatistics loose = new TraceCorrelationStatistics(lenient);
        assertEquals(0.6, loose.quantileOfSameElution(4, 30 * NOISE, NOISE, 0.05), 1e-6,
                "precondition: this is what the pairs say");
        assertEquals(few, loose.cutoffFor(4, 30 * NOISE, NOISE, 0.05), 1e-9,
                "but the cutoff stays at the minimum");

        // while a learned distribution stricter than the minimum is followed
        final float[][] strict = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(strict, new float[0]);
        final float stricter = (float) Math.min(0.999, many + 0.005);
        strict[TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE)] =
                filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, stricter);
        assertEquals(stricter, new TraceCorrelationStatistics(strict).cutoffFor(20, 30 * NOISE, NOISE, 0.05), 1e-6,
                "learning may raise the bar, which is the direction it is evidence for");
    }

    /**
     * A sample that could not calibrate borrows the distribution the experiment as a whole can measure.
     * <p>
     * Pooling across samples is sound because the bins are relative to each sample's own noise level rather than
     * absolute, so pairs from a sensitive run and a dull one land in the same bin when they are equally far above
     * what their own run can see. On a dilution series this is the difference between a cutoff and no cutoff: the
     * concentrated samples have isotope pairs well above the noise, the diluted ones have none, and the compounds are
     * the same throughout.
     * <p>
     * It is a fallback and not a merge. A sample that can read a condition keeps its own distribution there, or the
     * many weak pairs of the diluted samples would drag down the cutoff of the concentrated ones - the pooling would
     * cost exactly the samples it was supposed to leave alone.
     */
    @Test
    public void aSampleThatCannotCalibrateBorrowsThePooledDistribution() {
        // one sample with too few pairs to read anything, one with plenty
        final float[][] thin = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(thin, new float[0]);
        thin[TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE)] = filled(5, 0.97f);
        final TraceCorrelationStatistics diluted = new TraceCorrelationStatistics(thin);
        assertTrue(Double.isNaN(diluted.quantileOfSameElution(20, 30 * NOISE, NOISE, 0.05)),
                "precondition: five pairs cannot be read");

        final float[][] fat = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(fat, new float[0]);
        fat[TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE)] =
                filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, 0.97f);
        final TraceCorrelationStatistics concentrated = new TraceCorrelationStatistics(fat);

        final TraceCorrelationStatistics pooled =
                TraceCorrelationStatistics.pooled(List.of(diluted, concentrated));
        assertEquals(0.97, pooled.quantileOfSameElution(20, 30 * NOISE, NOISE, 0.05), 1e-6,
                "together they can be read");

        // the diluted sample now gets a cutoff where alone it had only the floor
        assertFalse(diluted.hasLearnedCutoffFor(20, 30 * NOISE, NOISE),
                "alone it has no evidence about this regime, and reports as much");
        assertEquals(TraceCorrelationStatistics.minimumCutoff(20),
                diluted.cutoffFor(20, 30 * NOISE, NOISE, 0.05), 1e-9, "so it answers with its own floor");
        assertTrue(diluted.fallingBackTo(pooled).hasLearnedCutoffFor(20, 30 * NOISE, NOISE),
                "with the pooled distribution behind it the regime is measured after all");
        assertEquals(0.97, diluted.fallingBackTo(pooled).cutoffFor(20, 30 * NOISE, NOISE, 0.05), 1e-6,
                "and the answer is what the experiment measured");
    }

    /** a sample that can read a condition is not overwritten in it, however much the pool holds */
    @Test
    public void aSampleThatCanCalibrateKeepsItsOwnDistribution() {
        final float[][] own = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(own, new float[0]);
        own[TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE)] =
                filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 2, 0.99f);
        final TraceCorrelationStatistics strict = new TraceCorrelationStatistics(own);

        final float[][] loose = new float[TraceCorrelationStatistics.binCount()][];
        Arrays.fill(loose, new float[0]);
        loose[TraceCorrelationStatistics.binOf(20, 30 * NOISE, NOISE)] =
                filled(TraceCorrelationStatistics.MIN_OBSERVATIONS * 20, 0.91f);
        final TraceCorrelationStatistics pool = new TraceCorrelationStatistics(loose);

        assertEquals(0.99, strict.fallingBackTo(pool).cutoffFor(20, 30 * NOISE, NOISE, 0.05), 1e-6,
                "its own measurement stands, and the pool does not lower it");
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
