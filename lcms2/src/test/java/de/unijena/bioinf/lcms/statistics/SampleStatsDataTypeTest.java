package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every field of {@link SampleStats} has to survive the store it is kept in.
 * <p>
 * Two of them used to be left out of the datatype, so whether they survived depended on whether the store happened to
 * hand back the instance that was put in or one it had deserialised - which means the bug was invisible in a test that
 * only writes and reads without forcing the store to do any work. Committing and reopening the map is what makes it
 * deterministic, so that is what this does.
 */
public class SampleStatsDataTypeTest {

    private static SampleStats sample() {
        final SampleStats stats = SampleStats.builder()
                .ms2NoiseLevel(1234.5f)
                .noiseLevelPerScan(new float[]{10f, 20f, 30f})
                .minimumIntensity(42.25f)
                .ms1MassDeviationWithinTraces(new Deviation(8, 3e-4))
                .minimumMs1MassDeviationBetweenTraces(new Deviation(10, 5e-4))
                .build();
        stats.setExpectedPeakWidth(7.5);
        return stats;
    }

    @Test
    public void everyFieldSurvivesTheStore() {
        final SampleStats original = sample();
        final SampleStatsDataType type = new SampleStatsDataType();
        // go through the datatype itself, which is what the store does, rather than through the store's caching
        final org.h2.mvstore.WriteBuffer buffer = new org.h2.mvstore.WriteBuffer(256);
        type.write(buffer, original);
        final java.nio.ByteBuffer read = buffer.getBuffer().flip();
        final SampleStats restored = type.read(read);

        assertEquals(original.getMs2NoiseLevel(), restored.getMs2NoiseLevel(), 0f);
        assertArrayEquals(original.getNoiseLevelPerScan(), restored.getNoiseLevelPerScan(), 0f);
        assertEquals(original.getMs1MassDeviationWithinTraces().getPpm(),
                restored.getMs1MassDeviationWithinTraces().getPpm(), 1e-12);
        assertEquals(original.getMs1MassDeviationWithinTraces().getAbsolute(),
                restored.getMs1MassDeviationWithinTraces().getAbsolute(), 1e-12);
        assertEquals(original.getMinimumMs1MassDeviationBetweenTraces().getPpm(),
                restored.getMinimumMs1MassDeviationBetweenTraces().getPpm(), 1e-12);
        assertEquals(original.getMinimumMs1MassDeviationBetweenTraces().getAbsolute(),
                restored.getMinimumMs1MassDeviationBetweenTraces().getAbsolute(), 1e-12);
        // the two that used to be dropped
        assertEquals(42.25f, restored.getMinimumIntensity(), 0f, "the minimum intensity has to survive");
        assertTrue(restored.getExpectedPeakWidth().isPresent(), "and so does the expected peak width");
        assertEquals(7.5, restored.getExpectedPeakWidth().get(), 1e-12);
    }

    @Test
    public void anAbsentPeakWidthStaysAbsent() {
        // an unset peak width is -1 and reports itself as empty; it must not come back as a peak width of zero,
        // which every rule that reads it would treat as a real number
        final SampleStats stats = SampleStats.builder()
                .ms2NoiseLevel(1f).noiseLevelPerScan(new float[]{1f}).minimumIntensity(1f)
                .ms1MassDeviationWithinTraces(new Deviation(8, 3e-4))
                .minimumMs1MassDeviationBetweenTraces(new Deviation(10, 5e-4))
                .build();
        assertTrue(stats.getExpectedPeakWidth().isEmpty(), "precondition: nothing was set");

        final SampleStatsDataType type = new SampleStatsDataType();
        final org.h2.mvstore.WriteBuffer buffer = new org.h2.mvstore.WriteBuffer(256);
        type.write(buffer, stats);
        final SampleStats restored = type.read(buffer.getBuffer().flip());

        assertTrue(restored.getExpectedPeakWidth().isEmpty(), "and it is still absent afterwards");
    }
}
