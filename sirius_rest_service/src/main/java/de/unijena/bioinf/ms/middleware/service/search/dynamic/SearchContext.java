package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import de.unijena.bioinf.ms.middleware.service.search.IndexedFields;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.function.Function;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface SearchContext extends Closeable {
    Path getIndexRootDir();

    <T> Integer getNumberOfDocuments(Class<T> clazz);

    <T> void addDocument(T bean);

    <T> void addDocuments(Collection<T> beans);

    <T> void updateDocument(T bean);

    <T> void updateDocuments(Collection<T> beans);

    <T> Optional<T> updateDocumentFields(@NotNull Object objectId, @NotNull Consumer<T> objectModifier, @NotNull Class<T> clazz) throws IllegalArgumentException;

    <T> void updateDocumentsFields(@NotNull Collection<?> objectIds, @NotNull Consumer<T> objectModifier, @NotNull Class<T> clazz);

    <T extends Taggable> void addTagsToDocuments(Map<String, ? extends Collection<? extends Tag>> docIdsToTags, @NotNull Class<T> clazz);

    <T extends Taggable> void addTagsToDocument(Object docId, Collection<Tag> tag, @NotNull Class<T> clazz);

    <T extends Taggable> void removeTagsFromDocument(Object docId, Collection<String> tagName, @NotNull Class<T> clazz);

    <T> void removeDocument(@NotNull T pojoToRemove);

    <T> void removeDocuments(@NotNull Collection<T> pojoToRemove);

    <T> void removeDocumentById(@NotNull Object documentId, Class<T> clazz);

    <T> void removeDocumentsById(@NotNull Collection<?> documentId, Class<T> clazz);

    /**
     * Mark the index for the given type complete/incomplete. An incomplete index (e.g. during a rebuild) is
     * not persisted on close and is rebuilt on the next open.
     */
    <T> void setIndexComplete(Class<T> clazz, boolean complete);

    <T> Page<T> search(@Nullable String query, Pageable pageable, Class<T> beanClass);

    <T> Page<String> searchIds(@Nullable String query, Pageable pageable, Class<T> beanClass);

    /**
     * Reads the given fields of the matching objects without reconstructing them.
     */
    <T, R> Page<R> searchFields(@Nullable String query, Pageable pageable, Class<T> beanClass,
                                Set<String> fields, Function<IndexedFields, R> mapper);

    ValueType getTagValueType(String tagName);

    void addTagValueType(String tagName, ValueType valueType);

    boolean removeTagValueType(String tagName);

    void close(boolean delete) throws IOException;

    default void close() throws IOException{
        close(false);
    }
}
