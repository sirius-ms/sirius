package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.features.Run;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Quantification tables refer to runs by id and name. To map such a result back to the data it was computed
 * from, a client has to be able to find out which file a run has been imported from.
 */
class RunSourceMappingTest {

    @AutoClose
    private NitriteSirirusProject ps;
    private NoSQLProjectImpl project;

    @BeforeEach
    void createTestProject() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
        project = new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), null, (a, b) -> false);
    }

    @SneakyThrows
    private Run convert(LCMSRun run) {
        return project.convertToApiRun(run, EnumSet.noneOf(Run.OptField.class));
    }

    @Test
    @DisplayName("a run reports the file it has been imported from")
    void runReportsItsSourceFile() {
        Run run = convert(LCMSRun.builder()
                .runId(1L)
                .name("my sample")
                .sourceReference(new MsDataSourceReference(
                        URI.create("file:///data/measurements/"), "sample-01.mzML", "run-1", "mzml-1"))
                .build());

        //the empty authority of the file URI is normalized away when location and file name are resolved
        assertEquals("file:/data/measurements/sample-01.mzML", run.getSource());
        assertEquals("my sample", run.getName(), "the name must stay untouched by the source mapping");
    }

    @Test
    @DisplayName("the file name alone is enough to identify the input")
    void runWithoutSourceLocationFallsBackToTheFileName() {
        Run run = convert(LCMSRun.builder()
                .runId(2L)
                .name("my blank")
                .sourceReference(new MsDataSourceReference((URI) null, "blank-01.mzML", null, null))
                .build());

        assertEquals("blank-01.mzML", run.getSource());
    }

    @Test
    @DisplayName("runs of directly imported data have no source")
    void runWithoutSourceReferenceHasNoSource() {
        Run run = convert(LCMSRun.builder().runId(3L).name("imported").build());

        assertNull(run.getSource());
    }
}
