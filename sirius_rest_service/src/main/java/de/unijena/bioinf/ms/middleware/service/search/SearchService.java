package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.SneakyThrows;

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


    ValueType getTagValueType(String projectId, String tagName);
    Stream<ValueType> getTagValueType(String projectId);
    void addTagValueType(String projectId, String tagName, ValueType valueType);
    boolean removeTagValueType(String projectId, String tagName);
}
