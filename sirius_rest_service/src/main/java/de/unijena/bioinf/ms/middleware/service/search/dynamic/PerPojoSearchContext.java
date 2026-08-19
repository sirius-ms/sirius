package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Set;
import java.util.function.Function;
import de.unijena.bioinf.ms.middleware.service.search.IndexedFields;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class PerPojoSearchContext implements SearchContext {
    @Getter
    @Nullable
    protected final Path indexRootDir;

    protected final ConcurrentHashMap<Class<?>, SinglePojoLuceneIndexManager<?>> indices;
    protected final Map<String, ValueType> tagDefs;

    public PerPojoSearchContext(@Nullable Path indexRootDir, @Nullable Map<String, ValueType> tagDefinitions) {
        this.indexRootDir = indexRootDir;
        indices = new ConcurrentHashMap<>();
        tagDefs = tagDefinitions != null ? tagDefinitions : new HashMap<>();
    }

    @Override
    public <T> Integer getNumberOfDocuments(Class<T> clazz) {
        return getIndexManager(clazz).getNumOfDocs().key();
    }

    @Override
    public <T> void addDocument(@NotNull T bean) {
        getIndexManager(bean).addDocument(bean);
    }

    @Override
    public <T> void addDocuments(@NotNull Collection<T> beans) {
        if (beans.isEmpty())
            return;
        getIndexManager(beans.iterator().next()).addDocuments(beans);
    }

    @Override
    public <T> void updateDocument(@NotNull T bean) {
        getIndexManager(bean).updateDocument(bean);
    }

    @Override
    public <T> void updateDocuments(@NotNull Collection<T> beans) {
        if (beans.isEmpty())
            return;
        getIndexManager(beans.iterator().next()).updateDocuments(beans);
    }

    /**
     * Updates the stored fields of a document.
     * <p>
     * This implementation reads the Lucene document, maps it to a bean, then applies the modifier and re-indexes the document.
     * IMPORTANT: to use these method all indexed field of the bean need to be stored in the index.
     * If this is not the case please get a fresh copy of the bean from the data source (db) and use the usual update method.
     *
     * @throws IllegalArgumentException If any field is indexed but not stored.
     *                                  .
     */

    @Override
    public <T> Optional<T> updateDocumentFields(@NotNull Object objectId, @NotNull Consumer<T> objectModifier, @NotNull Class<T> clazz) throws IllegalArgumentException {
        return getIndexManager(clazz).updateDocumentFields(objectId, objectModifier);
    }

    /**
     * Updates the stored fields of multiple documents.
     * <p>
     * This implementation reads the Lucene documents, maps each of them to a bean, then applies the modifier and re-indexes the documents.
     * IMPORTANT: to use these method all indexed field of the bean need to be stored in the index.
     * If this is not the case please get a fresh copy of the bean from the data source (db) and use the usual update method.
     *
     * @throws IllegalArgumentException If any field is indexed but not stored.
     *                                  .
     */
    @Override
    public <T> void updateDocumentsFields(@NotNull Collection<?> objectIds, @NotNull Consumer<T> objectModifier, @NotNull Class<T> clazz) {
        if (objectIds.isEmpty())
            return;
        getIndexManager(clazz).updateDocumentsFields(objectIds, objectModifier);
    }

    @Override
    public <T extends Taggable> void addTagsToDocuments(Map<String, ? extends Collection<? extends Tag>> docIdsToTags, @NotNull Class<T> clazz) {
        getIndexManager(clazz).addTagsToDocuments(docIdsToTags);
    }

    @Override
    public <T extends Taggable> void addTagsToDocument(Object docId, Collection<Tag> tags, @NotNull Class<T> clazz) {
        getIndexManager(clazz).addTagsToDocument(docId, tags);
    }

    @Override
    public <T extends Taggable> void removeTagsFromDocument(Object docId, Collection<String> tagNames, @NotNull Class<T> clazz) {
        getIndexManager(clazz).removeTagsFromDocument(docId, tagNames);
    }

    @Override
    public <T> void removeDocument(@NotNull T pojoToRemove) {
        getIndexManager(pojoToRemove).deleteDocument(pojoToRemove);
    }

    @Override
    public <T> void removeDocuments(@NotNull Collection<T> pojosToRemove) {
        if (pojosToRemove.isEmpty())
            return;
        getIndexManager(pojosToRemove.iterator().next()).deleteDocuments(pojosToRemove);
    }

    @Override
    public <T> void removeDocumentById(@NotNull Object documentId, Class<T> clazz) {
        getIndexManager(clazz).deleteDocumentById(documentId);
    }

    @Override
    public <T> void removeDocumentsById(@NotNull Collection<?> documentId, Class<T> clazz) {
        getIndexManager(clazz).deleteDocumentsById(documentId);
    }

    @Override
    public <T> void setIndexComplete(Class<T> clazz, boolean complete) {
        getIndexManager(clazz).setComplete(complete);
    }

    @Override
    public <T> Page<T> search(@Nullable String query, Pageable pageable, Class<T> beanClass) {
        return getIndexManager(beanClass).search(query, pageable);
    }

    @Override
    public <T> Page<String> searchIds(@Nullable String query, Pageable pageable, Class<T> beanClass) {
        return getIndexManager(beanClass).searchIds(query, pageable);
    }

    public <T, R> Page<R> searchFields(@Nullable String query, Pageable pageable, Class<T> beanClass,
                                       Set<String> fields, Function<IndexedFields, R> mapper) {
        return getIndexManager(beanClass).searchFields(query, pageable, fields, mapper);
    }

    @Override
    public <T> IndexSchema getIndexSchema(Class<T> beanClass) {
        // Objects without a document id field have no search index at all - they hold nothing.
        if (!GenericPojoMapper.isIndexable(beanClass))
            return IndexSchema.EMPTY;
        return getIndexManager(beanClass).getIndexSchema();
    }

    @Override
    public <T> Set<String> getMaterializedFieldNames(Class<T> beanClass) {
        if (!GenericPojoMapper.isIndexable(beanClass))
            return Set.of();
        return getIndexManager(beanClass).getIndexedFieldNames();
    }

    @Override
    public Map<String, ValueType> getTagValueTypes() {
        synchronized (tagDefs) {
            return Map.copyOf(tagDefs);
        }
    }

    /**
     * @return the value type of the given tag, or {@code null} if no definition for it has been registered
     * with this context. Callers must handle the null: this is a cache of the project's tag definitions, so a
     * tag can legitimately be unknown here (e.g. before its definition has been registered).
     */
    @Nullable
    @Override
    public ValueType getTagValueType(String tagName) {
        synchronized (tagDefs) {
            return tagDefs.get(tagName);
        }
    }

    @Override
    public void addTagValueType(String tagName, ValueType valueType) {
        synchronized (tagDefs) {
            // first add ValueType then propagate changes to indices
            ValueType valueTypeOld = tagDefs.putIfAbsent(tagName, valueType);
            if (valueTypeOld == null)
                indices.values().stream().filter(SinglePojoLuceneIndexManager::isTaggable)
                        .forEach(im -> im.addTagValueType(tagName, valueType));
            else
                log.debug("Tag value type '{}' is already registered; ignoring re-registration.", tagName);
        }
    }

    @Override
    public boolean removeTagValueType(String tagName) {
        synchronized (tagDefs) {
            // first propagate remove to indices then remove ValueType
            if (tagDefs.containsKey(tagName)) {
                indices.values().stream().filter(SinglePojoLuceneIndexManager::isTaggable)
                        .forEach(im -> im.removeTagValueType(tagName));
                return tagDefs.remove(tagName) != null;
            }
        }
        return false;
    }

    protected <T> Directory createIndexDirectory(Class<T> pojoClass) {
        //init lucene index.
        Directory directory = null;
        if (this.getIndexRootDir() != null) {
            try {
                Path indexPath = this.getIndexRootDir().resolve(pojoClass.getSimpleName());
                Files.createDirectories(indexPath);
                directory = FSDirectory.open(indexPath);
            } catch (IOException e) {
                log.error("Error when creating/opening index directory. Falling back to in memory index!", e);
            }
        }

        if (directory == null) {
            directory = new ByteBuffersDirectory();
            //todo move to debug
            log.info("Using in memory index for: {}", pojoClass.getSimpleName());
        }

        return directory;
    }

    @SuppressWarnings("unchecked")
    private <T> SinglePojoLuceneIndexManager<T> getIndexManager(T pojo) {
        return getIndexManager((Class<T>) pojo.getClass());
    }

    @SuppressWarnings("unchecked")
    private  <T> SinglePojoLuceneIndexManager<T> getIndexManager(Class<T> pojoClass) {
        return (SinglePojoLuceneIndexManager<T>) indices.computeIfAbsent(pojoClass, pc -> {
            // Pass an immutable snapshot of the tag definitions: the manager iterates them during
            // construction, which would otherwise race with concurrent addTagValueType/removeTagValueType.
            Map<String, ValueType> tagDefSnapshot;
            synchronized (tagDefs) {
                tagDefSnapshot = new HashMap<>(tagDefs);
            }
            return new SinglePojoLuceneIndexManager<>(createIndexDirectory(pc), pc, tagDefSnapshot, this::getTagValueType);
        });
    }

    @Override
    public void close(boolean delete) throws IOException {
        indices.forEachEntry(Long.MAX_VALUE, it -> {
            if (it.getValue() != null) {
                try {
                    it.getValue().close();
                } catch (IOException e) {
                    log.error("Error when closing index manager for pojo: {}", it.getKey().getSimpleName(), e);
                }
            }
        });

        if (delete && indexRootDir != null)
            FileUtils.deleteRecursively(indexRootDir);
    }
}
