package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.QuantRowType;
import de.unijena.bioinf.ms.middleware.model.features.QuantTable;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedIsotopicFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what a quantification table contains: rows are the quantified objects, columns are the runs they have been
 * detected in, and a value that could not be quantified is NaN. Objects that belong to no run at all are not
 * quantifiable, so a project without any of them has no table.
 */
class QuantTableContentTest {

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;
    private SearchService searchService;

    private long blankRunId;
    private long sampleRunId;
    private long caffeineId;
    private long glucoseId;
    private long compoundId;

    @BeforeEach
    void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        searchService = makeSearchService();
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);
    }

    @SneakyThrows
    private static SearchService makeSearchService() {
        return new SearchServiceImpl(p -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : p.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
    }

    /**
     * Two runs and two features. Caffeine was detected in both runs, glucose only in the sample run, so its blank
     * value is not quantifiable. Both features belong to the same compound.
     */
    @SneakyThrows
    private void importTwoRunsWithTwoFeatures() {
        blankRunId = insertRun("blank-01");
        sampleRunId = insertRun("sample-01");

        compoundId = insertCompound("my compound");
        caffeineId = insertAlignedFeature("caffeine", compoundId);
        glucoseId = insertAlignedFeature("glucose", compoundId);

        insertFeature(caffeineId, blankRunId, 10d, 100d);
        insertFeature(caffeineId, sampleRunId, 20d, 200d);
        insertFeature(glucoseId, sampleRunId, 30d, 300d);
    }

    @SneakyThrows
    private long insertRun(String name) {
        LCMSRun run = LCMSRun.builder().name(name).build();
        ps.getStorage().insert(run);
        return run.getRunId();
    }

    @SneakyThrows
    private long insertCompound(String name) {
        Compound compound = Compound.builder().name(name).build();
        ps.getStorage().insert(compound);
        return compound.getCompoundId();
    }

    @SneakyThrows
    private long insertAlignedFeature(String name, long compoundId) {
        AlignedFeatures feature = AlignedFeatures.builder().name(name).compoundId(compoundId).build();
        ps.getStorage().insert(feature);

        searchService.addDocument("test", de.unijena.bioinf.ms.middleware.model.features.AlignedFeature.builder()
                .alignedFeatureId(Long.toString(feature.getAlignedFeatureId()))
                .compoundId(Long.toString(compoundId))
                .name(name)
                .build());

        return feature.getAlignedFeatureId();
    }

    /**
     * An isotope of an aligned feature is stored as its own alignment with its own id, and its features reference
     * that id in the very same field an ordinary feature uses. It is not an aligned feature, so it is not indexed.
     *
     * @return id of the isotopic alignment
     */
    @SneakyThrows
    private long insertIsotopicAlignmentWithFeature(long runId, double apexIntensity) {
        AlignedIsotopicFeatures isotope = AlignedIsotopicFeatures.builder().build();
        ps.getStorage().insert(isotope);
        insertFeature(isotope.getAlignedIsotopeFeatureId(), runId, apexIntensity, apexIntensity * 10);
        return isotope.getAlignedIsotopeFeatureId();
    }

    @SneakyThrows
    private void insertFeature(long alignedFeatureId, long runId, double apexIntensity, double areaUnderCurve) {
        Feature feature = Feature.builder()
                .alignedFeatureId(alignedFeatureId)
                .runId(runId)
                .apexIntensity(apexIntensity)
                .areaUnderCurve(areaUnderCurve)
                .build();
        ps.getStorage().insert(feature);
    }

    private QuantTable featureTable(QuantMeasure measure) {
        Optional<QuantTable> table = project.getFeatureQuantification(null, measure);
        assertTrue(table.isPresent(), "the project has quantifiable features");
        return table.get();
    }

    @Test
    @DisplayName("columns are the runs of the project")
    void columnsAreRuns() {
        importTwoRunsWithTwoFeatures();
        QuantTable table = featureTable(QuantMeasure.APEX_INTENSITY);

        assertArrayEquals(new String[]{Long.toString(blankRunId), Long.toString(sampleRunId)}, table.getColumnIds());
        assertArrayEquals(new String[]{"blank-01", "sample-01"}, table.getColumnNames());
        assertEquals(QuantRowType.FEATURES, table.getRowType());
    }

    @Test
    @DisplayName("rows are the features with their quantities per run")
    void featureRowsCarryTheQuantities() {
        importTwoRunsWithTwoFeatures();
        QuantTable table = featureTable(QuantMeasure.APEX_INTENSITY);

        assertArrayEquals(new String[]{Long.toString(caffeineId), Long.toString(glucoseId)}, table.getRowIds());
        assertArrayEquals(new double[]{10d, 20d}, table.getValues()[0]);
        assertEquals(30d, table.getValues()[1][1]);
    }

    @Test
    @DisplayName("a feature that was not detected in a run has no value there")
    void missingQuantitiesAreNaN() {
        importTwoRunsWithTwoFeatures();
        QuantTable table = featureTable(QuantMeasure.APEX_INTENSITY);

        assertTrue(Double.isNaN(table.getValues()[1][0]), "glucose was not detected in the blank run");
    }

    @Test
    @DisplayName("the area under the curve can be quantified as well")
    void areaUnderCurveIsSupported() {
        importTwoRunsWithTwoFeatures();
        QuantTable table = featureTable(QuantMeasure.AREA_UNDER_CURVE);

        assertArrayEquals(new double[]{100d, 200d}, table.getValues()[0]);
        assertEquals(QuantMeasure.AREA_UNDER_CURVE, table.getQuantificationMeasure());
    }

    @Test
    @DisplayName("a compound is quantified with the sum of its features per run")
    void compoundQuantitiesAreSummedPerRun() {
        importTwoRunsWithTwoFeatures();
        Optional<QuantTable> table = project.getCompoundQuantification(null, QuantMeasure.APEX_INTENSITY);

        assertTrue(table.isPresent());
        assertArrayEquals(new String[]{Long.toString(compoundId)}, table.get().getRowIds());
        assertEquals(QuantRowType.COMPOUNDS, table.get().getRowType());
        //caffeine only in the blank run, caffeine and glucose in the sample run
        assertArrayEquals(new double[]{10d, 50d}, table.get().getValues()[0]);
    }

    @Test
    @DisplayName("only the selected compounds are quantified")
    void compoundSelectionIsApplied() {
        importTwoRunsWithTwoFeatures();
        Optional<QuantTable> table = project.getCompoundQuantification("NOT compoundId:" + compoundId, QuantMeasure.APEX_INTENSITY);

        assertTrue(table.isEmpty(), "the only compound of the project is excluded");
    }

    @Test
    @DisplayName("features that belong to no run cannot be quantified")
    void featuresWithoutRunsAreNotQuantifiable() {
        insertRun("sample-01");
        insertAlignedFeature("orphan", 0L);

        assertTrue(project.getFeatureQuantification(null, QuantMeasure.APEX_INTENSITY).isEmpty());
    }

    @Test
    @DisplayName("a project without runs has no quantification table")
    void projectWithoutRunsHasNoTable() {
        assertTrue(project.getFeatureQuantification(null, QuantMeasure.APEX_INTENSITY).isEmpty());
        assertTrue(project.getCompoundQuantification(null, QuantMeasure.APEX_INTENSITY).isEmpty());
    }

    @Test
    @DisplayName("rows are ordered by id, like the columns are")
    void rowsAreOrderedById() {
        importTwoRunsWithTwoFeatures();
        QuantTable table = featureTable(QuantMeasure.APEX_INTENSITY);

        assertEquals(List.of(Long.toString(caffeineId), Long.toString(glucoseId)), List.of(table.getRowIds()));
        assertTrue(caffeineId < glucoseId, "the fixture relies on ascending ids");
    }

    @Test
    @DisplayName("a feature that belongs to no run is no row of the table")
    void featuresWithoutRunsAreNoRows() {
        importTwoRunsWithTwoFeatures();
        insertAlignedFeature("never detected", compoundId);

        QuantTable table = featureTable(QuantMeasure.APEX_INTENSITY);

        assertEquals(2, table.getRowIds().length, "only features with quantities are rows");
        assertArrayEquals(new String[]{Long.toString(caffeineId), Long.toString(glucoseId)}, table.getRowIds());
    }
    @Test
    @DisplayName("isotopes of a feature are not rows of their own")
    void isotopicAlignmentsAreNoRows() {
        importTwoRunsWithTwoFeatures();
        long isotopeId = insertIsotopicAlignmentWithFeature(sampleRunId, 5d);

        QuantTable table = featureTable(QuantMeasure.APEX_INTENSITY);

        assertArrayEquals(new String[]{Long.toString(caffeineId), Long.toString(glucoseId)}, sorted(table.getRowIds()),
                "only aligned features are quantified, isotope " + isotopeId + " must not appear");
    }

    private static String[] sorted(String[] ids) {
        String[] copy = ids.clone();
        java.util.Arrays.sort(copy);
        return copy;
    }

}
