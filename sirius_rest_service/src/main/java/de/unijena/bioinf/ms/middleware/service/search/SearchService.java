package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface SearchService extends AutoCloseable {
    void openOrCreateProjectIndex(Project<?> project) throws IOException;

    @SneakyThrows
    default void closeProjectIndex(String projectId) {
        //sneaky throws is fine since no deletion will happen.
        closeProjectIndex(projectId, false);
    }

    void closeProjectIndex(String projectId, boolean deleteIndexFromDisk) throws IOException;

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
    <T extends Taggable> void addTagsToDocuments(@NotNull String projectId, Collection<?> docId, Collection<Tag> tags, @NotNull Class<T> clazz);

    default <T extends Taggable> void removeTagFromDocument(@NotNull String projectId, Object docId, String tagName, @NotNull Class<T> clazz){
        removeTagsFromDocument(projectId,docId, List.of(tagName), clazz);
    }

    <T extends Taggable> void removeTagsFromDocument(@NotNull String projectId, Object docId,  Collection<String> tagName, @NotNull Class<T> clazz);

    <T> void removeDocument(@NotNull String projectId, @NotNull T beanToRemove);
    <T> void removeDocuments(@NotNull String projectId, @NotNull Collection<T> beans);

    <T> void removeDocumentById(@NotNull String projectId, @NotNull Object docId, Class<T> pojoClass);

    <T> void removeDocumentsById(@NotNull String projectId, @NotNull Collection<?> docIds, Class<T> pojoClass);

    <T> Page<T> search(String projectId, @Nullable String query, Pageable pageable, Class<T> beanClass);
    <T> Page<String> searchIds(String projectId, @Nullable String query, Pageable pageable, Class<T> beanClass);

    ValueType getTagValueType(String projectId, String tagName);
    void addTagValueType(String projectId, String tagName, ValueType valueType);
    boolean removeTagValueType(String projectId, String tagName);
}
