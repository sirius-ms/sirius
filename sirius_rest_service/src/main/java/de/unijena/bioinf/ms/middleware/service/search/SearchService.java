package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import lombok.SneakyThrows;
import de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;

public interface SearchService extends Closeable {
    void openOrCreateProjectIndex(Project<?> project) throws IOException;
    @SneakyThrows
    default void closeProjectIndex(String projectId) {
        //sneaky throws is fine since no deletion will happen.
        closeProjectIndex(projectId, false);
    }

    void closeProjectIndex(String projectId, boolean deleteIndexFromDisk) throws IOException;
    SearchIndexReader getSearchIndexReader();
    SearchIndexWriter getSearchIndexWriter();


    TagDefinition getTagDefinition(String projectId, String tagName);
    Stream<TagDefinition> getTagDefinitions(String projectId);
    void addTagDefinition(String projectId, TagDefinition tagDefinition);
    boolean removeTagDefinition(String projectId, String tagName);

    //replace with lucene based handling.
    @Deprecated(forRemoval = true)
    Filter parseFindTagsByObjectType(String projectId, Class<?> targeObjectClass, String luceneFilterQuery) throws QueryNodeException, IOException;
    //replace with  lucene based handling.
    @Deprecated(forRemoval = true)
    Filter parseFindTags(String projectId, String luceneFilterQuery) throws QueryNodeException, IOException;

}
