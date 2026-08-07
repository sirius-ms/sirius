package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.ms.middleware.model.compute.LcmsSubmissionParameters;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A query string cannot express null, so the deprecated endpoints that take the import parameters as query
 * parameters receive an empty string where a sample name should be derived from its input file. Everywhere
 * else a blank sample name stays a user error.
 */
class ProjectControllerSampleNameTest {

    @Test
    void blankSampleNamesBecomeMissingNames() {
        LcmsSubmissionParameters params = new LcmsSubmissionParameters();
        params.setSampleNames(Arrays.asList("my sample", "", "  ", null, "my blank"));

        ProjectController.treatBlankSampleNamesAsMissing(params);

        assertEquals(Arrays.asList("my sample", null, null, null, "my blank"), params.getSampleNames());
        assertDoesNotThrow(() -> params.validate(5), "normalized names must pass validation");
    }

    @Test
    void keepsSampleNamesWithoutBlanks() {
        LcmsSubmissionParameters params = new LcmsSubmissionParameters();
        params.setSampleNames(List.of("my sample", "my blank"));

        ProjectController.treatBlankSampleNamesAsMissing(params);

        assertEquals(List.of("my sample", "my blank"), params.getSampleNames());
    }

    @Test
    void toleratesMissingParametersAndNames() {
        assertDoesNotThrow(() -> ProjectController.treatBlankSampleNamesAsMissing(null));
        assertDoesNotThrow(() -> ProjectController.treatBlankSampleNamesAsMissing(new LcmsSubmissionParameters()));
    }
}
