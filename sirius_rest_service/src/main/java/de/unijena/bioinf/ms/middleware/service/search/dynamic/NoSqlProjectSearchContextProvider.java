package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.search.ApiDocFieldDescriptions;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class NoSqlProjectSearchContextProvider implements SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> {

    private final Path indexingHome;
    private final boolean deleteIndexingHomeOnExit;

    public NoSqlProjectSearchContextProvider(boolean inMemoryIndex, @Nullable Path indexingHome, boolean deleteIndexingHomeOnExit) {
        this.deleteIndexingHomeOnExit = deleteIndexingHomeOnExit;

        if (inMemoryIndex) {
            this.indexingHome = null;
        } else {
            if (indexingHome == null) {
                try {
                    indexingHome = Files.createTempDirectory("sirius-search-indexes");
                } catch (IOException e) {
                    log.error("Error when creating temp storage for project indices!", e);
                }
            }
            this.indexingHome = indexingHome;
        }

        if (this.indexingHome == null)
            log.warn("Running in in-memory search index mode.");
    }

    @Override
    public PerPojoDatabaseSearchContext<?> create(NoSQLProjectImpl project) {
        Path projectIndexRoot = null;

        if (indexingHome != null) {
            try {
                String projectSystemId = project.getSystemUID(); //used as index name.
                projectIndexRoot = indexingHome.resolve(projectSystemId);
                Files.createDirectories(projectIndexRoot);
            } catch (IOException e) {
                log.error("Error when creating indexing dir for {}. Falling back to in memory index!", projectIndexRoot, e);
                projectIndexRoot = null;
            }
        }

        Map<String, ValueType> tagDefinitions = new HashMap<>();
        project.project().findAllTagDefinitionsStr().forEach(td -> tagDefinitions.put(td.getTagName(), td.getValueType()));

        // Field descriptions come from the API documentation of the models (schema annotations/javadoc);
        // injected here (REST-side glue) so the search machinery itself stays free of presentation concerns.
        return new PerPojoDatabaseSearchContext<>(project.storage(), projectIndexRoot, tagDefinitions, ApiDocFieldDescriptions.PROVIDER);
    }

    @Override
    public void destroy() {
        if (deleteIndexingHomeOnExit && indexingHome != null) {
            try {
                FileUtils.deleteRecursively(indexingHome);
            } catch (IOException e) {
                log.error("Error when deleting temporary indexing home: {}", indexingHome, e);
            }
        }
    }
}
