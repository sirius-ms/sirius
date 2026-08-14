package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinitionImport;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The search context caches tag value types, and the Lucene {@code TagMapper} needs the type of a tag to pick
 * the field encoding. Registration used to happen only through the {@code onInsert} storage event, which Nitrite
 * dispatches on its own thread, so a caller that defined a tag and immediately tagged an object could index
 * against an empty cache and NPE on the missing {@link ValueType}. That is what broke
 * {@code NoSQLProjectTest#testTags} on CI while passing locally.
 */
public class TagValueTypeRegistrationTest {

    private NitriteSirirusProject ps;

    @BeforeEach
    public void createProjectSpace() throws IOException {
        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        ps = new NitriteSirirusProject(location);
    }

    @AfterEach
    public void closeProjectSpace() {
        try {
            ps.close();
        } catch (Exception ignored) {
            // best effort
        }
    }

    @Test
    public void createTagsRegistersTheValueTypeBeforeReturning() {
        List<Thread> registeringThreads = Collections.synchronizedList(new ArrayList<>());
        SearchService searchService = Mockito.mock(SearchService.class);
        Mockito.doAnswer(invocation -> {
            registeringThreads.add(Thread.currentThread());
            return null;
        }).when(searchService).addTagValueType(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        NoSQLProjectImpl project =
                new NoSQLProjectImpl("test", new NoSQLProjectSpaceManager(ps), searchService, (a, b) -> false);

        project.createTags(
                List.of(TagDefinitionImport.builder().tagName("c1").valueType(ValueType.BOOLEAN).build()), true);

        assertTrue(registeringThreads.contains(Thread.currentThread()),
                "createTags must register the tag value type with the search service on the calling thread. " +
                        "Relying on the asynchronous storage insert event leaves a window in which tagging an " +
                        "object with a freshly defined tag indexes it against a null ValueType and NPEs. " +
                        "Registering threads seen: " + registeringThreads);
    }
}
