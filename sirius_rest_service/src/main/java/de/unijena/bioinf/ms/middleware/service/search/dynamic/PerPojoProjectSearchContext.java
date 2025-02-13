package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
public class PerPojoProjectSearchContext implements ProjectSearchContext {
    @Getter
    @Nullable
    private final Path projectIndexRootDir;

    // Map from projectId → (bean class → IndexManager)
    private final ConcurrentHashMap<Class<?>, SinglePojoLuceneIndexManager<?>> indices;
    // Map from projectId → (tagName → ValueType)
    private final Map<String, ValueType> tagDefs;

    public PerPojoProjectSearchContext(@Nullable Path projectIndexRootDir, @Nullable Collection<TagDefinition> tagDefinitions) {
        this.projectIndexRootDir = projectIndexRootDir;
        indices = new ConcurrentHashMap<>();
        tagDefs = new HashMap<>();
        if (tagDefinitions != null)
                tagDefinitions.forEach(tagDef -> tagDefs.put(tagDef.getTagName(), tagDef.getValueType()));
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

    @Override
    public <T extends Taggable> void addTagsToDocuments(Collection<Object> docIds, Collection<Tag> tags, @NotNull Class<T> clazz) {
        getIndexManager(clazz).addTagsToDocuments(docIds, tags);
    }

    @Override
    public <T extends Taggable> void addTagsToDocument(Object docId, Collection<Tag> tags, @NotNull Class<T> clazz){
        getIndexManager(clazz).addTagsToDocument(docId, tags);
    }

    @Override
    public <T extends Taggable> void removeTagsFromDocument(Object docId, Collection<String> tagNames, @NotNull Class<T> clazz){
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
    public <T> void removeDocumentsById(@NotNull Collection<Object> documentId, Class<T> clazz) {
        getIndexManager(clazz).deleteDocumentsById(documentId);
    }

    @Override
    public <T> Page<T> search(@Nullable String query, Pageable pageable, Class<T> beanClass) {
        return getIndexManager(beanClass).search(query, pageable);
    }

    @Override
    public <T> Page<String> searchIds(@Nullable String query, Pageable pageable, Class<T> beanClass) {
        return getIndexManager(beanClass).searchIds(query, pageable);
    }

    @NotNull
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
                log.warn("Valuetype already existed bug was updated? should not happen.");
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

    @SuppressWarnings("unchecked")
    private <T> SinglePojoLuceneIndexManager<T> getIndexManager(T pojo) {
        return getIndexManager((Class<T>) pojo.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T> SinglePojoLuceneIndexManager<T> getIndexManager(Class<T> clazz) {
            return (SinglePojoLuceneIndexManager<T>) indices.computeIfAbsent(clazz,
                    c -> new SinglePojoLuceneIndexManager<>(this, tagDefs, c));
    }

    public static final Factory<PerPojoProjectSearchContext> FACTORY = (projectDir, project) -> {
        if (projectDir != null && !Files.exists(projectDir)) {
            try {
                Files.createDirectories(projectDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new PerPojoProjectSearchContext(projectDir, project.findTags());
    };

    @Override
    public void close() throws IOException {
        for (SinglePojoLuceneIndexManager<?> im : indices.values().stream().toList())
            if (im != null)
                im.close();
    }
}
