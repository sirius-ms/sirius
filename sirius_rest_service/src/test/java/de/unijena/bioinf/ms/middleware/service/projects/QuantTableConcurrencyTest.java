package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.QuantTable;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The features of a table are read by several threads at once, so a table has to come out exactly as it would from a
 * single thread. Feature rows are written by the thread that owns their aligned feature, but a compound row collects
 * the features of several aligned features and is therefore written by several threads.
 * <p>
 * The project is large enough that the work is spread over more than one worker, otherwise a race could not show.
 */
class QuantTableConcurrencyTest {

    private static final int RUNS = 12;
    private static final int COMPOUNDS = 40;
    /** every compound has this many aligned features, so its row is written by that many threads */
    private static final int FEATURES_PER_COMPOUND = 5;

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;
    private SearchService searchService;

    private final List<Long> runIds = new ArrayList<>();
    /** what the table must contain, computed while the fixture is written */
    private final Map<Long, double[]> expectedFeatureRows = new HashMap<>();
    private final Map<Long, double[]> expectedCompoundRows = new HashMap<>();

    @BeforeEach
    void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        searchService = makeSearchService();
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);

        for (int r = 0; r < RUNS; r++)
            runIds.add(insertRun("run-" + r));

        double intensity = 1;
        for (int c = 0; c < COMPOUNDS; c++) {
            long compoundId = insertCompound("compound-" + c);
            double[] compoundRow = new double[RUNS];
            for (int f = 0; f < FEATURES_PER_COMPOUND; f++) {
                long featureId = insertAlignedFeature("feature-" + c + "-" + f, compoundId);
                double[] featureRow = new double[RUNS];
                //every aligned feature is detected in every run, so every cell is written
                for (int r = 0; r < RUNS; r++) {
                    insertFeature(featureId, runIds.get(r), intensity, intensity * 10);
                    featureRow[r] = intensity;
                    compoundRow[r] += intensity;
                    intensity++;
                }
                expectedFeatureRows.put(featureId, featureRow);
            }
            expectedCompoundRows.put(compoundId, compoundRow);
        }
    }

    @Test
    @DisplayName("every feature row holds the quantities of its own feature")
    void featureRowsAreCorrectUnderConcurrency() {
        QuantTable table = project.getFeatureQuantification(null, QuantMeasure.APEX_INTENSITY,
                EnumSet.noneOf(QuantTable.OptField.class)).orElseThrow();

        assertEquals(expectedFeatureRows.size(), table.getRowIds().length, "one row per aligned feature");
        assertRows(expectedFeatureRows, table);
    }

    @Test
    @DisplayName("a compound row sums its features although several threads write it")
    void compoundRowsAreCorrectUnderConcurrency() {
        QuantTable table = project.getCompoundQuantification(null, QuantMeasure.APEX_INTENSITY,
                EnumSet.noneOf(QuantTable.OptField.class)).orElseThrow();

        assertEquals(expectedCompoundRows.size(), table.getRowIds().length, "one row per compound");
        assertRows(expectedCompoundRows, table);
    }

    @Test
    @DisplayName("reading the same table twice gives the same table")
    void repeatedReadsAgree() {
        QuantTable first = project.getFeatureQuantification(null, QuantMeasure.APEX_INTENSITY,
                EnumSet.noneOf(QuantTable.OptField.class)).orElseThrow();
        QuantTable second = project.getFeatureQuantification(null, QuantMeasure.APEX_INTENSITY,
                EnumSet.noneOf(QuantTable.OptField.class)).orElseThrow();

        assertArrayEquals(first.getRowIds(), second.getRowIds(), "rows must not depend on which thread was first");
        assertArrayEquals(first.getColumnIds(), second.getColumnIds());
        for (int row = 0; row < first.getValues().length; row++)
            assertArrayEquals(first.getValues()[row], second.getValues()[row]);
    }

    /** the table is ordered by id, so a row can be found by the position of its id */
    private void assertRows(Map<Long, double[]> expected, QuantTable table) {
        Map<String, Integer> columnAt = new HashMap<>();
        for (int i = 0; i < table.getColumnIds().length; i++)
            columnAt.put(table.getColumnIds()[i], i);

        for (int row = 0; row < table.getRowIds().length; row++) {
            long id = Long.parseLong(table.getRowIds()[row]);
            double[] want = expected.get(id);
            assertEquals(true, want != null, "unexpected row " + id);
            for (int r = 0; r < RUNS; r++) {
                int column = columnAt.get(Long.toString(runIds.get(r)));
                assertEquals(want[r], table.getValues()[row][column], 1e-9,
                        "row " + id + ", run " + runIds.get(r));
            }
        }
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

    @SneakyThrows
    private long insertRun(String name) {
        LCMSRun run = LCMSRun.builder()
                .name(name)
                .sourceReference(new MsDataSourceReference(URI.create("file:///data/"), name + ".mzML", null, null))
                .build();
        ps.getStorage().insert(run);
        searchService.addDocument("test", de.unijena.bioinf.ms.middleware.model.features.Run.builder()
                .runId(Long.toString(run.getRunId())).name(name).build());
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
        searchService.addDocument("test", AlignedFeature.builder()
                .alignedFeatureId(Long.toString(feature.getAlignedFeatureId()))
                .compoundId(Long.toString(compoundId))
                .name(name)
                .build());
        return feature.getAlignedFeatureId();
    }

    @SneakyThrows
    private void insertFeature(long alignedFeatureId, long runId, double apexIntensity, double areaUnderCurve) {
        ps.getStorage().insert(Feature.builder()
                .alignedFeatureId(alignedFeatureId)
                .runId(runId)
                .apexIntensity(apexIntensity)
                .areaUnderCurve(areaUnderCurve)
                .build());
    }
}
