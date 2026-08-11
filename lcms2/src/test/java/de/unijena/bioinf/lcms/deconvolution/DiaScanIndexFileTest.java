package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.io.MzMLParser;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.Chromatography;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Reads a real DIA file and reports the acquisition scheme we recover from it. Acquisition schemes differ
 * considerably between vendors and instruments, so this is the test to point at a new file when we add support
 * for one.
 * <p>
 * Skipped unless a file is given, either as the system property {@code sirius.dia.testfile} or as the environment
 * variable {@code SIRIUS_DIA_TEST_FILE}:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/to/file.mzML ./gradlew --no-daemon :lcms2:test --tests '*DiaScanIndexFileTest*' -i
 * </pre>
 */
public class DiaScanIndexFileTest {

    private static Optional<Path> testFile() {
        String location = System.getProperty("sirius.dia.testfile", System.getenv("SIRIUS_DIA_TEST_FILE"));
        if (location == null || location.isBlank()) return Optional.empty();
        Path path = Path.of(location);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    @Test
    public void reportAcquisitionSchemeOfRealFile() throws Exception {
        Optional<Path> file = testFile();
        assumeTrue(file.isPresent(), "no DIA test file given, set SIRIUS_DIA_TEST_FILE");

        final LCMSStorageFactory storageFactory = LCMSStorage.temporaryStorage(null, false);
        try {
            final ProcessedSample sample = new MzMLParser().parse(file.get(), storageFactory,
                    run -> {}, run -> {}, null, null,
                    LCMSRun.builder().chromatography(Chromatography.LC).build());

            final DiaScanIndex index = DiaScanIndexBuilder.build(sample).orElseThrow(
                    () -> new AssertionError("no DIA acquisition scheme found in " + file.get()));

            System.out.println("=== " + file.get().getFileName() + " ===");
            System.out.println("MS1 scans: " + sample.getMapping().length());
            System.out.print(index.describe());

            assertTrue(index.numberOfChannels() > 0);
            for (DiaChannel channel : index.getChannels()) {
                assertTrue(channel.numberOfScans() >= DiaScanIndexBuilder.MIN_SCANS_PER_CHANNEL);
                assertTrue(channel.medianScanInterval() > 0, "a channel must advance in retention time");
                for (int k = 1; k < channel.numberOfScans(); ++k) {
                    assertTrue(channel.getAxis().getRetentionTimeAt(k) > channel.getAxis().getRetentionTimeAt(k - 1));
                    assertTrue(channel.getToMs1Index()[k] >= channel.getToMs1Index()[k - 1],
                            "the MS1 scans a channel points at must not go backwards");
                }
                for (int w = 0; w < channel.numberOfWindows(); ++w) {
                    assertTrue(channel.windowUpperBound(w) > channel.windowLowerBound(w));
                }
            }
        } finally {
            storageFactory.close();
        }
    }
}
