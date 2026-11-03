package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.middleware.service.search.mappers.IndexSchema;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Set;
import de.unijena.bioinf.ms.middleware.service.search.IndexedFields;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A dynamic Lucene search service that implements SearchService.
 *
 * <ul>
 *   <li>It “annotates” POJO fields using {@code @DocumentId} and {@code @IndexField} (like Hibernate Search).</li>
 *   <li>It organizes Lucene indices by project and by POJO type.</li>
 *   <li>It allows adding “dynamic tag” fields to documents. Dynamic tag fields are named with the prefix
 *       {@code "tags."} followed by the tag name and their Lucene field type is determined from the
 *       project's registered {@link ValueType} for that tag.</li>
 *   <li>When mapping back from search results to a bean, fields that were not stored become {@code null}.</li>
 *   <li>The updateDocumentFields method will throw an IllegalArgumentException if any of the bean’s indexed fields are not stored.</li>
 *   <li>Tags are always stored.</li>
 * </ul>
 */
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final SearchContextProvider searchContextFactory;

    private final Map<String, SearchContext> projectSearchContexts = new HashMap<>();
    private final ReadWriteLock projectLock = new ReentrantReadWriteLock();

    public SearchServiceImpl(SearchContextProvider searchContextProvider) throws IOException {
        this.searchContextFactory = searchContextProvider;
    }


    public void close() {
        projectLock.writeLock().lock();
        try {
            for (SearchContext context : projectSearchContexts.values()) {
                try {
                    context.close();
                } catch (IOException e) {
                    log.error("Error closing project search context", e);
                }
            }
            projectSearchContexts.clear();
        } finally {
            projectLock.writeLock().unlock();
        }
    }

    @Override
    public void openOrCreateProjectIndex(Project<?> project) throws IOException {
        projectLock.writeLock().lock();
        try {
            projectSearchContexts.computeIfAbsent(project.getProjectId(),
                    pid -> searchContextFactory.create(project));
        } finally {
            projectLock.writeLock().unlock();
        }
    }

    @Override
    public void closeProjectIndex(@NotNull Project<?> project, boolean deleteIndex) throws IOException {
        projectLock.writeLock().lock();
        try {
            SearchContext projectContext = projectSearchContexts.remove(project.getProjectId());
            if (projectContext != null)
                projectContext.close(deleteIndex);
        } finally {
            projectLock.writeLock().unlock();
        }
    }

    @Override
    public void reanchorStorageCommitVersion(Project<?> project, java.nio.file.Path projectFile) throws IOException {
        if (project instanceof NoSQLProjectImpl) {
            de.unijena.bioinf.storage.db.nosql.Metadata metadata = de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase.buildMetadata();

            // Re-open a temporary database on the compacted file to update versions cleanly and safely.
            // The path was captured by the caller while the project was still open, so we neither depend on
            // the (now closed) project handle nor open a second handle on a file another connection owns.
            try (de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase tempDb = new de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase(projectFile, metadata)) {
                long currentVersion = tempDb.getStorageCommitId();
                if (currentVersion != -1) {
                    long targetVersion = currentVersion + 1;

                    // Eagerly load existing indices into a list to prevent concurrent modification / cursor infinite loops
                    List<PersistentSearchIndex> list = new ArrayList<>();
                    for (PersistentSearchIndex index : tempDb.findAll(PersistentSearchIndex.class)) {
                        list.add(index);
                    }

                    for (PersistentSearchIndex index : list) {
                        index.setStorageCommitId(targetVersion);
                        tempDb.upsert(index);
                    }
                    tempDb.flush();
                }
            }
        }
    }

    @Override
    public void clearIndex(@NotNull Project<?> project) throws IOException {
        projectLock.writeLock().lock();
        try {
            closeProjectIndex(project, true);
            openOrCreateProjectIndex(project);
        } finally {
            projectLock.writeLock().unlock();
        }
    }

    @Override
    public <T> int getNumberOfDocuments(String projectId, Class<T> clazz) {
        return withProjectContext(projectId, ps -> ps.getNumberOfDocuments(clazz));
    }

    @Override
    public <T> void addDocument(String projectId, T bean) {
        consumeProjectContext(projectId, ps -> ps.addDocument(bean));
    }

    @Override
    public <T> void addDocuments(String projectId, Collection<T> beans) {
        consumeProjectContext(projectId, ps -> ps.addDocuments(beans));
    }

    @Override
    public <T> void updateDocument(String projectId, T bean) {
        consumeProjectContext(projectId, ps -> ps.updateDocument(bean));
    }

    @Override
    public <T> void updateDocuments(String projectId, Collection<T> beans) {
        consumeProjectContext(projectId, ps -> ps.updateDocuments(beans));
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
    public <T> Optional<T> updateDocumentFields(@NotNull String projectId, Object objectId, Consumer<T> objectModifier, Class<T> clazz) throws IllegalArgumentException {
        return withProjectContext(projectId, ps -> ps.updateDocumentFields(objectId, objectModifier, clazz));
    }

    @Override
    public <T> void updateDocumentsFields(@NotNull String projectId, Collection<?> objectIds, Consumer<T> objectModifier, Class<T> clazz) throws IllegalArgumentException {
        consumeProjectContext(projectId, ps -> ps.updateDocumentsFields(objectIds, objectModifier, clazz));
    }

    @Override
    public <T extends Taggable> void addTagsToDocument(@NotNull String projectId, Object docId, Collection<Tag> tags, @NotNull Class<T> clazz) {
        consumeProjectContext(projectId, ps -> ps.addTagsToDocument(docId, tags, clazz));
    }

    @Override
    public <T extends Taggable> void addTagsToDocuments(@NotNull String projectId, Map<String, ? extends Collection<? extends Tag>> docIdsToTags, @NotNull Class<T> clazz) {
        consumeProjectContext(projectId, ps -> ps.addTagsToDocuments(docIdsToTags, clazz));
    }

    @Override
    public <T extends Taggable> void removeTagsFromDocument(@NotNull String projectId, Object docId, Collection<String> tagName, @NotNull Class<T> clazz) {
        consumeProjectContext(projectId, ps -> ps.removeTagsFromDocument(docId, tagName, clazz));
    }

    @Override
    public <T> void removeDocument(@NotNull String projectId, @NotNull T pojoToRemove) {
        consumeProjectContext(projectId, ps -> ps.removeDocument(pojoToRemove));
    }

    @Override
    public <T> void removeDocuments(@NotNull String projectId, @NotNull Collection<T> pojos) {
        consumeProjectContext(projectId, ps -> ps.removeDocuments(pojos));
    }

    @Override
    public <T> void removeDocumentById(@NotNull String projectId, @NotNull Object docId, Class<T> pojoClass) {
        consumeProjectContext(projectId, ps -> ps.removeDocumentById(docId, pojoClass));
    }

    @Override
    public <T> void removeDocumentsById(@NotNull String projectId, @NotNull Collection<?> docIds, Class<T> pojoClass) {
        consumeProjectContext(projectId, ps -> ps.removeDocumentsById(docIds, pojoClass));
    }

    @Override
    public <T> void setIndexComplete(String projectId, Class<T> clazz, boolean complete) {
        consumeProjectContext(projectId, ps -> ps.setIndexComplete(clazz, complete));
    }

    @Override
    public <T> void makeWritesSearchable(String projectId, Class<T> clazz) {
        consumeProjectContext(projectId, ps -> ps.makeWritesSearchable(clazz));
    }

    @Override
    public <T> Page<T> search(String projectId, @Nullable String query, Pageable pageable, Class<T> pojoClass) {
        return withProjectContext(projectId, ps -> ps.search(query, pageable, pojoClass));
    }

    @Override
    public <T> Page<String> searchIds(String projectId, @Nullable String query, Pageable pageable, Class<T> pojoClass) {
        return withProjectContext(projectId, ps -> ps.searchIds(query, pageable, pojoClass));
    }

    @Override
    public <T, R> Page<R> searchFields(String projectId, @Nullable String query, Pageable pageable, Class<T> pojoClass,
                                       Set<String> fields, Function<IndexedFields, R> mapper) {
        return withProjectContext(projectId, ps -> ps.searchFields(query, pageable, pojoClass, fields, mapper));
    }

    @Override
    public <T> IndexSchema getIndexSchema(String projectId, Class<T> pojoClass) {
        return withProjectContext(projectId, ps -> ps.getIndexSchema(pojoClass));
    }

    @Override
    public <T> Set<String> getMaterializedFieldNames(String projectId, Class<T> pojoClass) {
        return withProjectContext(projectId, ps -> ps.getMaterializedFieldNames(pojoClass));
    }

    @Override
    public Map<String, ValueType> getTagValueTypes(String projectId) {
        return withProjectContext(projectId, SearchContext::getTagValueTypes);
    }

    @Override
    public ValueType getTagValueType(String projectId, String tagName) {
        return withProjectContext(projectId, ps -> ps.getTagValueType(tagName));

    }

    @Override
    public void addTagValueType(String projectId, String tagName, ValueType valueType) {
        consumeProjectContext(projectId, ps -> ps.addTagValueType(tagName, valueType));
    }

    @Override
    public boolean removeTagValueType(String projectId, String tagName) {
        return withProjectContext(projectId, ps -> ps.removeTagValueType(tagName));
    }

    // region HELPER

    private <T> T withProjectContext(String projectId, Function<SearchContext, T> function) {
        projectLock.readLock().lock();
        try {
            return function.apply(requireContext(projectId));
        } finally {
            projectLock.readLock().unlock();
        }
    }

    private void consumeProjectContext(String projectId, Consumer<SearchContext> consumer) {
        projectLock.readLock().lock();
        try {
            consumer.accept(requireContext(projectId));
        } finally {
            projectLock.readLock().unlock();
        }
    }

    @NotNull
    private SearchContext requireContext(String projectId) {
        SearchContext context = projectSearchContexts.get(projectId);
        if (context == null)
            throw new IllegalStateException("No open search index for project '" + projectId
                    + "'. The project index was not initialized or failed to open.");
        return context;
    }

    //endregion


}
