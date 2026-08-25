package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Where a fragmentation spectrum of a run without MS1 scans goes: which feature it is offered to, and how the
 * leftovers are collected into compounds. Both are pure computation on masses and retention times, so they are
 * pinned here rather than through a whole pipeline run.
 */
public class Ms2OnlyRunAssignmentTest {

    private final List<LCMSStorageFactory> storages = new ArrayList<>();
    private ProcessedSample sample;
    private int nextUid = 1;

    @AfterEach
    public void closeStorages() {
        storages.forEach(LCMSStorageFactory::close);
    }

    private ProcessedSample sample() {
        if (sample == null) try {
            final LCMSStorageFactory factory = LCMSStorage.temporaryStorage(null, false);
            storages.add(factory);
            final LCMSStorage storage = factory.createNewStorage();
            storage.setStatistics(SampleStats.builder().noiseLevelPerScan(new float[0]).ms2NoiseLevel(0f)
                    .minimumIntensity(0f).ms1MassDeviationWithinTraces(new Deviation(8, 3e-4))
                    .minimumMs1MassDeviationBetweenTraces(new Deviation(10, 3e-4)).build());
            sample = new ProcessedSample(new ScanPointMapping(new double[0], new int[0], null,
                    new Int2IntOpenHashMap()), storage, 1, 0);
            sample.setRun(LCMSRun.builder().name("ms2 only").build());
            sample.getRun().setRunId(7L);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return sample;
    }

    /** a chain of common neutral losses, which is what tells a fragmentation spectrum from noise */
    private static final double[] FRAGMENTS = {55.0291, 83.0240, 109.0396, 137.0709, 155.0815, 182.0924};
    private static final double[] INTENSITIES = {600, 9000, 4000, 10000, 1200, 800};

    private Ms2OnlyRunAssignment.Fragmentation fragmentation(double precursorMz, double retentionTime) {
        final SimpleMutableSpectrum peaks = new SimpleMutableSpectrum();
        for (int k = 0; k < FRAGMENTS.length; ++k) peaks.addPeak(FRAGMENTS[k], INTENSITIES[k]);
        return fragmentation(precursorMz, retentionTime, new SimpleSpectrum(peaks));
    }

    private Ms2OnlyRunAssignment.Fragmentation fragmentation(double precursorMz, double retentionTime,
                                                             SimpleSpectrum peaks) {
        final Ms2SpectrumHeader header = new Ms2SpectrumHeader(nextUid, nextUid++, "scan=" + nextUid, 1, 2, true,
                new CollisionEnergy(20), IsolationWindow.fromOffsets(0.5, 0.5), -1, precursorMz, precursorMz,
                retentionTime);
        return new Ms2OnlyRunAssignment.Fragmentation(sample(), header, peaks);
    }

    private static Ms2OnlyRunAssignment.FeatureTarget feature(long id, double mass, double from, double to) {
        return new Ms2OnlyRunAssignment.FeatureTarget(id, mass, from, to, (from + to) / 2);
    }

    @Test
    public void aSpectrumGoesToTheFeatureThatWasElutingAtItsPrecursorMass() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final Ms2OnlyRunAssignment.Result result = assignment.assign(
                List.of(fragmentation(195.0877, 122d)),
                List.of(feature(1L, 195.0879, 110d, 130d), feature(2L, 205.0972, 110d, 130d)));
        assertEquals(1, result.assignedSpectra());
        assertTrue(result.assigned().containsKey(1L));
        assertTrue(result.unassignedGroups().isEmpty());
    }

    @Test
    public void aSpectrumTakenWhereNothingElutedIsLeftOver() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final Ms2OnlyRunAssignment.Result result = assignment.assign(
                List.of(fragmentation(195.0877, 300d)),
                List.of(feature(1L, 195.0879, 110d, 130d)));
        assertEquals(0, result.assignedSpectra());
    }

    /**
     * The retention times come from a different measurement than the features do, so a spectrum a little outside
     * the feature's own interval still belongs to it.
     */
    @Test
    public void aSpectrumJustOutsideTheFeaturesIntervalStillBelongsToIt() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        assertEquals(1, assignment.assign(List.of(fragmentation(195.0877, 138d)),
                List.of(feature(1L, 195.0877, 110d, 130d))).assignedSpectra());
        assertEquals(0, assignment.assign(List.of(fragmentation(195.0877, 145d)),
                List.of(feature(1L, 195.0877, 110d, 130d))).assignedSpectra());
    }

    /** two features of the same mass elute one after the other; the spectrum belongs to the one it was taken in */
    @Test
    public void ofTwoFeaturesOfTheSameMassTheOneClosestInRetentionTimeWins() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final Ms2OnlyRunAssignment.Result result = assignment.assign(
                List.of(fragmentation(195.0877, 260d)),
                List.of(feature(1L, 195.0877, 110d, 130d), feature(2L, 195.0877, 250d, 270d)));
        assertTrue(result.assigned().containsKey(2L));
    }

    @Test
    public void aSpectrumThatNamesNoPrecursorIsCountedAndNotUsed() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final Ms2OnlyRunAssignment.Result result = assignment.assign(List.of(fragmentation(0d, 120d)), List.of());
        assertEquals(1, result.withoutPrecursor());
        assertTrue(result.unassignedGroups().isEmpty());
    }

    /**
     * The same bar the data dependent path applies before an MS/MS is attached to a trace: a spectrum that would
     * have been thrown away there is thrown away here, whether or not it happens to fit a feature.
     */
    @Test
    public void aSpectrumWithoutAnySignalIsNeitherAssignedNorKept() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final SimpleMutableSpectrum noise = new SimpleMutableSpectrum();
        noise.addPeak(101.3311, 110);
        noise.addPeak(133.7712, 130);
        final Ms2OnlyRunAssignment.Result result = assignment.assign(
                List.of(fragmentation(195.0877, 120d, new SimpleSpectrum(noise))),
                List.of(feature(1L, 195.0877, 110d, 130d)));
        assertEquals(0, result.assignedSpectra());
        assertTrue(result.unassignedGroups().isEmpty());
        assertEquals(1, result.discardedSpectra());
    }

    @Test
    public void repeatedFragmentationOfOnePrecursorIsOneGroup() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final List<List<Ms2OnlyRunAssignment.Fragmentation>> groups = assignment.group(List.of(
                fragmentation(195.0877, 120d), fragmentation(195.0879, 126d), fragmentation(195.0876, 133d)));
        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).size());
    }

    @Test
    public void aGapLongerThanTheToleranceSplitsAGroup() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final List<List<Ms2OnlyRunAssignment.Fragmentation>> groups = assignment.group(List.of(
                fragmentation(195.0877, 120d), fragmentation(195.0877, 131d)));
        assertEquals(2, groups.size());
    }

    @Test
    public void differentPrecursorsAreDifferentGroupsEvenAtTheSameRetentionTime() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final List<List<Ms2OnlyRunAssignment.Fragmentation>> groups = assignment.group(List.of(
                fragmentation(195.0877, 120d), fragmentation(205.0972, 120d)));
        assertEquals(2, groups.size());
    }

    /** the outcome must not depend on the order the spectra or the features happen to arrive in */
    @Test
    public void theOutcomeDoesNotDependOnTheOrderOfTheInput() {
        final Ms2OnlyRunAssignment assignment = new Ms2OnlyRunAssignment();
        final Ms2OnlyRunAssignment.Fragmentation a = fragmentation(195.0877, 120d);
        final Ms2OnlyRunAssignment.Fragmentation b = fragmentation(205.0972, 240d);
        final List<Ms2OnlyRunAssignment.FeatureTarget> features = List.of(
                feature(1L, 195.0877, 110d, 130d), feature(2L, 205.0972, 230d, 250d));
        final Ms2OnlyRunAssignment.Result forward = assignment.assign(List.of(a, b), features);
        final Ms2OnlyRunAssignment.Result backward = assignment.assign(List.of(b, a),
                List.of(features.get(1), features.get(0)));
        assertEquals(forward.assigned().keySet(), backward.assigned().keySet());
    }
}
