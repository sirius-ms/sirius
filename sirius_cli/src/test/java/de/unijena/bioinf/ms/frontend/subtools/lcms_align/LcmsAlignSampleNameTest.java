package de.unijena.bioinf.ms.frontend.subtools.lcms_align;

import de.unijena.bioinf.lcms.utils.Tracker;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinitions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Sample names and sample types are matched to the input files by index. Getting this wrong silently
 * mislabels runs, so it is pinned here.
 */
class LcmsAlignSampleNameTest {

    private static final List<Path> THREE_FILES = List.of(Path.of("a.mzml"), Path.of("b.mzml"), Path.of("c.mzml"));

    @Test
    void namesAreMatchedByIndex() {
        LcmsAlignSubToolJobNoSql job = job(List.of("first", "second", "third"), null);

        assertEquals("first", job.sampleNameAt(0));
        assertEquals("second", job.sampleNameAt(1));
        assertEquals("third", job.sampleNameAt(2));
    }

    @Test
    void missingNamesFallBackToTheInputFile() {
        LcmsAlignSubToolJobNoSql job = job(Arrays.asList("first", null), null);

        assertEquals("first", job.sampleNameAt(0));
        assertNull(job.sampleNameAt(1), "null entries are derived from the input file");
        assertNull(job.sampleNameAt(2), "files without an entry are derived from the input file");
    }

    @Test
    void noNamesAtAllFallBackToTheInputFiles() {
        LcmsAlignSubToolJobNoSql job = job(null, null);

        assertNull(job.sampleNameAt(0));
        assertNull(job.sampleNameAt(2));
    }

    @Test
    void typesAreMatchedByIndex() {
        LcmsAlignSubToolJobNoSql job = job(null, List.of(
                TagDefinitions.SAMPLE_TYPE_BLANK, TagDefinitions.SAMPLE_TYPE_SAMPLE, TagDefinitions.SAMPLE_TYPE_BLANK));

        assertEquals(TagDefinitions.SAMPLE_TYPE_BLANK, job.sampleTypeAt(0));
        assertEquals(TagDefinitions.SAMPLE_TYPE_SAMPLE, job.sampleTypeAt(1));
        assertEquals(TagDefinitions.SAMPLE_TYPE_BLANK, job.sampleTypeAt(2));
    }

    @Test
    void missingTypesFallBackToSample() {
        LcmsAlignSubToolJobNoSql job = job(null, null);

        assertEquals(TagDefinitions.SAMPLE_TYPE_SAMPLE, job.sampleTypeAt(0));
        assertEquals(TagDefinitions.SAMPLE_TYPE_SAMPLE, job.sampleTypeAt(2));
    }

    private static LcmsAlignSubToolJobNoSql job(List<String> sampleNames, List<String> sampleTypes) {
        return new LcmsAlignSubToolJobNoSql(THREE_FILES, sampleNames, sampleTypes, () -> null, true,
                DataSmoothing.AUTO, 0.5, 8, -1, 3, null, null, false, new Tracker.NOOP());
    }
}
