package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.function.Function;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface SearchService extends AutoCloseable {
    void openOrCreateProjectIndex(Project<?> project) throws IOException;

    @SneakyThrows
    default void closeProjectIndex(@NotNull Project<?> project) {
        //sneaky throws is fine since no deletion will happen.
        closeProjectIndex(project, false);
    }

    void closeProjectIndex(Project<?> project, boolean deleteIndex) throws IOException;

    /**
     * Reanchor the persisted index versions to the given (already-closed) project file. The caller must pass
     * the project file path, captured while the project was still open, so this never depends on the project
     * being open and never holds a second handle on a file that another connection still owns.
     */
    void reanchorStorageCommitVersion(Project<?> project, Path projectFile) throws IOException;

    void clearIndex(@NotNull Project<?> project) throws IOException;

    <T> int getNumberOfDocuments(String projectId, Class<T> clazz);

    default <T>  boolean isEmpty (String projectId, Class<T> clazz){
        return getNumberOfDocuments(projectId, clazz) <= 0;
    }

    <T> void addDocument(String projectId, T bean);

    <T> void addDocuments(String projectId, Collection<T> beans);


    <T> void updateDocument(String projectId, T bean);

    <T> void updateDocuments(String projectId, Collection<T> beans);

    <T> Optional<T> updateDocumentFields(@NotNull String projectId, Object uuid, Consumer<T> modifier, Class<T> clazz);

    default <T extends Taggable> void addTagToDocument(@NotNull String projectId, Object docId, Tag tag, @NotNull Class<T> clazz){
        addTagsToDocument(projectId,docId, List.of(tag), clazz);
    }

    <T> void updateDocumentsFields(@NotNull String projectId, Collection<?> objectIds, Consumer<T> objectModifier, Class<T> clazz) throws IllegalArgumentException;

    <T extends Taggable> void addTagsToDocument(@NotNull String projectId, Object docId, Collection<Tag> tags, @NotNull Class<T> clazz);
    <T extends Taggable> void addTagsToDocuments(@NotNull String projectId, Map<String, ? extends Collection<? extends Tag>> docIdsToTags, @NotNull Class<T> clazz);

    default <T extends Taggable> void removeTagFromDocument(@NotNull String projectId, Object docId, String tagName, @NotNull Class<T> clazz){
        removeTagsFromDocument(projectId,docId, List.of(tagName), clazz);
    }

    <T extends Taggable> void removeTagsFromDocument(@NotNull String projectId, Object docId,  Collection<String> tagName, @NotNull Class<T> clazz);

    <T> void removeDocument(@NotNull String projectId, @NotNull T beanToRemove);
    <T> void removeDocuments(@NotNull String projectId, @NotNull Collection<T> beans);

    <T> void removeDocumentById(@NotNull String projectId, @NotNull Object docId, Class<T> pojoClass);

    <T> void removeDocumentsById(@NotNull String projectId, @NotNull Collection<?> docIds, Class<T> pojoClass);

    /**
     * Mark the index for the given type complete/incomplete for a project. An incomplete index is not
     * persisted on close and is rebuilt on the next open (used to bracket (re)builds).
     */
    <T> void setIndexComplete(String projectId, Class<T> clazz, boolean complete);

    <T> Page<T> search(String projectId, @Nullable String query, Pageable pageable, Class<T> beanClass);
    <T> Page<String> searchIds(String projectId, @Nullable String query, Pageable pageable, Class<T> beanClass);

    /**
     * Reads the given fields of the matching objects without reconstructing them. Use it to look up a few properties
     * of many objects, e.g. to relate ids to each other.
     *
     * @param fields fields to read, the mapper only sees these
     * @param mapper builds the result of a hit from its fields
     */
    <T, R> Page<R> searchFields(String projectId, @Nullable String query, Pageable pageable, Class<T> beanClass,
                                Set<String> fields, Function<IndexedFields, R> mapper);

    /** What the given project's index of that type holds, as recorded when it was configured. */
    <T> IndexSchema getIndexSchema(String projectId, Class<T> beanClass);

    /** The field names actually present in that index, including the keys a dynamic field has taken. */
    <T> Set<String> getMaterializedFieldNames(String projectId, Class<T> beanClass);

    /** The tag definitions the given project knows, by name. */
    Map<String, ValueType> getTagValueTypes(String projectId);

    ValueType getTagValueType(String projectId, String tagName);
    void addTagValueType(String projectId, String tagName, ValueType valueType);
    boolean removeTagValueType(String projectId, String tagName);
}
