package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.QueryRewriter;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.service.search.mappers.LuceneMappingUtils.*;

/**
 * A helper class that manages one Lucene index (i.e. one Directory, IndexWriter, and searcher)
 * for one POJO type (within one project). It supports:
 * <ul>
 *   <li>Indexing of primitive/wrapper types, Strings, enums (as keywords).</li>
 *   <li>Indexing of collections/arrays by adding multiple fields.</li>
 *   <li>Indexing nested objects by scanning their @IndexField–annotated fields and prefixing the field name.</li>
 * </ul>
 */
@Slf4j
class SinglePojoLuceneIndexManager<T> implements Closeable {
    private final Directory directory;
    private final IndexWriter writer;
    private final SearcherManager searcherManager;
    private final SnapshotDeletionPolicy snapshotDeletionPolicy;

    /**
     * Whether this index is known to fully reflect the source data. Defaults to true (a loaded or empty index
     * is trusted); set to false while it is being (re)built and whenever a write fails, so an incomplete index
     * is not persisted and gets rebuilt on the next open. See M5/M4.
     */
    @Setter
    @Getter
    private volatile boolean complete = true;

    private final GenericPojoMapper<T> pojoMapper;

    // Guards the (non-thread-safe) query parser and the query-config maps (pointsConfigMap, fieldAnalyzers)
    // against concurrent query parsing and tag-config mutation. Index writes serialize on 'this' instead;
    // reads run lock-free via the thread-safe SearcherManager.
    private final Object configLock = new Object();

    private final Map<String, QueryRewriter> queryRewriters = new HashMap<>();

    // Base analyzer and dynamic (per-field) analyzer:
    private final Analyzer baseAnalyzer = SIRIUS_TEXT_ANALYZER;
    private final Map<String, Analyzer> fieldAnalyzers = new PrefixAwareString2ObjectHashMap<>();
    private final Map<String, PointsConfig> pointsConfigMap = new PrefixAwareString2ObjectHashMap<>();
    private final Map<String, SortField.Type> sortTypes = new PrefixAwareString2ObjectHashMap<>();
    private final PerFieldAnalyzerWrapper dynamicAnalyzer = new PerFieldAnalyzerWrapper(baseAnalyzer, fieldAnalyzers);

    // StandardQueryParser using the dynamic analyzer.
    private final List<CharSequence> defaultSearchFields = new ArrayList<>();
    private final StandardQueryParser queryParser = new StandardQueryParser(dynamicAnalyzer);

    public SinglePojoLuceneIndexManager(@NotNull Directory directory,
                                        @NotNull Class<T> pojoClass,
                                        @Nullable Map<String, ValueType> initialValueTypes,
                                        @NotNull Function<String, ValueType> tagValueTypeProvider
    ) {
        try {
            this.directory = directory;
            this.pojoMapper = new GenericPojoMapper<>(pojoClass, new TagMapper(tagValueTypeProvider));
            IndexWriterConfig writerConfig = new IndexWriterConfig(dynamicAnalyzer);
            // Protect commit files from background-merge deletion while we serialize a snapshot (see getIndexData).
            this.snapshotDeletionPolicy = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
            writerConfig.setIndexDeletionPolicy(snapshotDeletionPolicy);
            this.writer = new IndexWriter(directory, writerConfig);
            this.searcherManager = new SearcherManager(writer, null);

            // Scans add all existing TagDefinitions to set PointConfigs and Analyzers
            if (initialValueTypes != null)
                initialValueTypes.forEach(this::addTagValueType);

            // Scans the bean class’s @IndexField annotations to set PointConfigs and Analyzers.
            // Adds PointsConfig entries to the query parser.
            // Adds default search fields to query parser
            pojoMapper.detectAnalyzersAndPointConfigs(pointsConfigMap, fieldAnalyzers, defaultSearchFields, sortTypes, queryRewriters);
            // Initialize the query with  points config map and default search fields.
            queryParser.setMultiFields(defaultSearchFields.toArray(CharSequence[]::new));
            queryParser.setPointsConfigMap(pointsConfigMap);
        } catch (IOException e) {
            throw new RuntimeException("IO Error when initializing Lucene index for " + pojoClass.getSimpleName(), e);
        }
    }

    public boolean isEmpty() {
        return getNumOfDocs().key() <= 0;
    }

    public boolean isTaggable() {
        return Taggable.class.isAssignableFrom(pojoMapper.getPojoClass());
    }

    public synchronized byte[] getIndexData(){
        return getIndexData(false);
    }

    @SneakyThrows
    public synchronized byte[] getIndexData(boolean zipped){
        // Ensure all pending Lucene writes are flushed & committed before we read the directory files
        writer.commit();
        // Snapshot the commit so its files cannot be deleted by a concurrent background merge while we copy them.
        IndexCommit commit = snapshotDeletionPolicy.snapshot();
        try {
            return LuceneDirectoryPersistenceUtils.serialize(directory, commit.getFileNames(), zipped);
        } finally {
            snapshotDeletionPolicy.release(commit);
        }
    }

    @SneakyThrows
    public Pair<Integer, Integer> getNumOfDocs() {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            int numdocs = searcher.getIndexReader().numDocs();
            int maxdocs = searcher.getIndexReader().maxDoc();
            return Pair.of(numdocs, maxdocs);
        } finally {
            searcherManager.release(searcher);
        }
    }

    /**
     * Runs a Lucene write, marking the index incomplete if it fails so the drift is not persisted and the
     * index is rebuilt on the next open (M4). Checked exceptions are rethrown transparently.
     */
    private void doWrite(WriterAction action) {
        try {
            action.run();
        } catch (Throwable t) {
            complete = false;
            throw lombok.Lombok.sneakyThrow(t);
        }
    }

    @FunctionalInterface
    private interface WriterAction {
        void run() throws IOException;
    }

    // low level lucene document handling.
    // NOTE: writes are not committed per call; visibility for readers is provided by the NRT SearcherManager
    // (maybeRefresh on read). Durability is ensured on close()/getIndexData(). This makes bulk ingest fast.
    public synchronized void addDocument(@NotNull T pojo) {
        Document doc = pojoMapper.toDocument(pojo);
        doWrite(() -> writer.addDocument(doc));
    }

    public synchronized void addDocuments(@NotNull Collection<T> pojos) {
        if (pojos.isEmpty())
            return;
        List<Document> docs = pojos.stream().map(pojoMapper::toDocument).toList();
        doWrite(() -> writer.addDocuments(docs));
    }

    public synchronized void updateDocument(@NotNull T pojo) {
        Document doc = pojoMapper.toDocument(pojo);
        doWrite(() -> writer.updateDocument(new Term(pojoMapper.getPojoIdField(), doc.get(pojoMapper.getPojoIdField())), doc));
    }

    public synchronized void updateDocuments(@NotNull Collection<T> pojos) {
        if (pojos.isEmpty())
            return;
        List<Document> docs = pojos.stream()
                .map(pojoMapper::toDocument)
                .toList();

        Query q = new TermInSetQuery(pojoMapper.getPojoIdField(), docs.stream()
                .map(doc -> doc.get(pojoMapper.getPojoIdField()))
                .map(Object::toString)
                .map(BytesRef::new)
                .collect(Collectors.toSet()));

        doWrite(() -> writer.updateDocuments(q, docs));
    }

    public synchronized void deleteDocument(@NotNull T pojoToRemove) {
        deleteDocumentById(pojoMapper.getIdValue(pojoToRemove));
    }

    public synchronized void deleteDocuments(@NotNull Collection<T> pojosToRemove) {
        deleteDocumentsById(pojosToRemove.stream().map(pojoMapper::getIdValue).toList());
    }

    public synchronized void deleteDocumentById(@NotNull Object id) {
        doWrite(() -> writer.deleteDocuments(new Term(pojoMapper.getPojoIdField(), String.valueOf(id))));
    }

    public synchronized void deleteDocumentsById(@NotNull Collection<?> ids) {
        if (ids.isEmpty())
            return;
        Query q = new TermInSetQuery(pojoMapper.getPojoIdField(), ids.stream().map(Object::toString).map(BytesRef::new).collect(Collectors.toSet()));
        doWrite(() -> writer.deleteDocuments(q));
    }

    public synchronized void updateDocumentsFields(Collection<?> docIds, Consumer<T> modifier) throws IllegalArgumentException {
        if (pojoMapper.isNonStoredFields()) {
            String msg = String.format(NON_STORED_FIELDS_MESSAGE, pojoMapper.getPojoClass().getSimpleName(), docIds);
            log.warn(msg);
            throw new UnsupportedOperationException(msg);
        }

        Query q = new TermInSetQuery(pojoMapper.getPojoIdField(), docIds.stream()
                .map(Object::toString).map(BytesRef::new)
                .collect(Collectors.toSet()));

        List<T> pojos = search(q, Pageable.unpaged())
                .stream()
                .peek(modifier)
                .toList();

        updateDocuments(pojos);
    }

    private static final String NON_STORED_FIELDS_MESSAGE = "Indexed object (%s) with id '%s' contains indexed fields that are not stored! "
            + "Partial update not supported. Please provide the full object to perform an Update.";

    /**
     * Updates the stored fields of a document.
     * If any indexed field is not stored, an exception is thrown.
     */
    public synchronized Optional<T> updateDocumentFields(Object docId, Consumer<T> modifier) throws IllegalArgumentException {
        if (pojoMapper.isNonStoredFields()) {
            String msg = String.format(NON_STORED_FIELDS_MESSAGE, pojoMapper.getPojoClass().getSimpleName(), docId);
            log.warn(msg);
            throw new UnsupportedOperationException(msg);
        }

        Document doc = searchDocumentById(docId);
        if (doc == null)
            return Optional.empty();

        T pojo = pojoMapper.toPojo(doc);
        modifier.accept(pojo);
        updateDocument(pojo);
        return Optional.of(pojo);
    }

    public synchronized void addTagsToDocuments(Map<String, ? extends Collection<? extends Tag>> docIdsToTags) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot add tags to non Taggable Object! %s does not implement Taggable!", pojoMapper.getPojoName()));

        updateDocumentsFields(docIdsToTags.keySet(), pojo -> {
            Map<String, Tag> tagMap = ((Taggable) pojo).getTags();
            if (tagMap == null)
                tagMap = new HashMap<>();

            for (Tag tag : docIdsToTags.get((String) pojoMapper.getIdValue(pojo)))
                tagMap.put(tag.getTagName(), tag);

            ((Taggable) pojo).setTags(tagMap);
        });

    }

    public synchronized void addTagsToDocument(Object docId, Collection<Tag> tags) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot add tags to non Taggable Object! %s does not implement Taggable!", pojoMapper.getPojoName()));


        updateDocumentFields(docId, pojo -> {
            Map<String, Tag> tagMap = ((Taggable) pojo).getTags();
            if (tagMap == null)
                tagMap = new HashMap<>();

            for (Tag tag : tags)
                tagMap.put(tag.getTagName(), tag);

            ((Taggable) pojo).setTags(tagMap);
        });
    }

    public synchronized void removeTagsFromDocument(Object docId, Collection<String> tagNames) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot remove tags from non Taggable Object! %s does not implement Taggable!", pojoMapper.getPojoName()));

        updateDocumentFields(docId, pojo -> {
            Map<String, Tag> tagMap = ((Taggable) pojo).getTags();
            if (tagMap != null)
                tagNames.forEach(tagMap::remove);
        });
    }

    @SneakyThrows
    public Page<T> search(@Nullable String query, Pageable pageable) {
        // parse under configLock (parser + config maps are not thread-safe); rewrite (may resolve
        // synonyms via network) and the actual search run lock-free.
        return search(rewriteQuery(parseQuery(query)), pageable);
    }

    /**
     * Searches the index using the given query and returns a Spring Data Page of beans.
     */
    @SneakyThrows
    public Page<T> search(Query query, Pageable pageable) {
        return searchAndTransform(query, pageable, pojoMapper::toPojo);
    }


    @SneakyThrows
    public Page<String> searchIds(@Nullable String query, Pageable pageable) {
        return searchIds(rewriteQuery(parseQuery(query)), pageable);
    }

    /**
     * Searches the index using the given query and returns a Spring Data Page of beans.
     */
    @SneakyThrows
    public Page<String> searchIds(Query query, Pageable pageable) {
        return searchAndTransform(query, pageable, doc -> doc.get(pojoMapper.getPojoIdField()));
    }

    @SneakyThrows
    private Query parseQuery(@Nullable String query) {
        if (Utils.isNullOrBlank(query))
            return new MatchAllDocsQuery();
        synchronized (configLock) {
            return queryParser.parse(query, null);
        }
    }

    private Query rewriteQuery(Query query) {
        // Wrapper queries: recurse into the wrapped query and preserve the wrapper (including its boost),
        // so a field rewriter still applies to a boosted or constant-score term/phrase.
        if (query instanceof BoostQuery boost) {
            Query inner = boost.getQuery();
            Query rewritten = rewriteQuery(inner);
            return rewritten == inner ? query : new BoostQuery(rewritten, boost.getBoost());
        } else if (query instanceof ConstantScoreQuery csq) {
            Query inner = csq.getQuery();
            Query rewritten = rewriteQuery(inner);
            return rewritten == inner ? query : new ConstantScoreQuery(rewritten);
        } else if (query instanceof BooleanQuery) {
            BooleanQuery bq = (BooleanQuery) query;
            boolean changed = false;
            List<BooleanClause> clauses = bq.clauses();
            List<BooleanClause> newClauses = new ArrayList<>(clauses.size());

            for (BooleanClause clause : clauses) {
                Query originalSub = clause.query();
                Query rewrittenSub = rewriteQuery(originalSub);

                if (rewrittenSub != originalSub) {
                    changed = true;
                    newClauses.add(new BooleanClause(rewrittenSub, clause.occur()));
                } else {
                    newClauses.add(clause);
                }
            }

            if (changed) {
                BooleanQuery.Builder builder = new BooleanQuery.Builder();
                builder.setMinimumNumberShouldMatch(bq.getMinimumNumberShouldMatch());
                for (BooleanClause c : newClauses) {
                    builder.add(c);
                }
                return builder.build();
            }
            return query;
        } else if (query instanceof TermQuery tq) {
            Query rewritten = rewriteTermOrPhrase(tq.getTerm().field(), tq.getTerm().text(), false);
            if (rewritten != null)
                return rewritten;
        } else if (query instanceof PhraseQuery pq) {
            Term[] terms = pq.getTerms();
            if (terms.length > 0) {
                String text = Arrays.stream(terms).map(Term::text).collect(Collectors.joining(" "));
                Query rewritten = rewriteTermOrPhrase(terms[0].field(), text, true);
                if (rewritten != null)
                    return rewritten;
            }
        }
        // WildcardQuery/PrefixQuery/range/MultiPhraseQuery are intentionally passed through: the registered
        // QueryRewriters operate on plain term/phrase text, so feeding them wildcard/range text would corrupt
        // the query (e.g. turn `name:foo*` into a literal term `foo*`). Such queries already work natively.
        return query;
    }

    private Query rewriteTermOrPhrase(String field, String text, boolean isPhrase) {
        QueryRewriter rewriter = queryRewriters.get(field);
        if (rewriter != null)
            return rewriter.rewrite(field, text, isPhrase);

        return null;
    }

    @SneakyThrows
    private <R> Page<R> searchAndTransform(Query query, Pageable pageable, Function<Document, R> function) {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            int numDocs = searcher.getIndexReader().numDocs();
            if (numDocs <= 0)
                return Page.empty(pageable);

            org.apache.lucene.search.Sort sort = convertToLuceneSort(pageable, sortTypes);
            int numHits = pageable.isUnpaged() ? numDocs : Math.min(numDocs, (int) (pageable.getOffset() + pageable.getPageSize()));
            //todo add search after mechanism for better deep pagination

            TopDocs topDocs = sort != null ? searcher.search(query, numHits, sort) : searcher.search(query, numHits);

            ScoreDoc[] hits = topDocs.scoreDocs;
            int start = pageable.isUnpaged() ? 0 : (int) pageable.getOffset();
            int end = pageable.isUnpaged() ? hits.length : Math.min(start + pageable.getPageSize(), hits.length);
            org.apache.lucene.index.StoredFields storedFields = searcher.storedFields();
            List<R> results = new ArrayList<>(end - start);
            for (int i = start; i < end; i++)
                results.add(function.apply(storedFields.document(hits[i].doc)));

            return new PageImpl<>(results, pageable, topDocs.totalHits.value());
        } finally {
            // Always release the searcher when done.
            searcherManager.release(searcher);
        }
    }

    @SneakyThrows
    @Nullable
    private Document searchDocumentById(Object id) {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            Query query = new TermQuery(new Term(pojoMapper.getPojoIdField(), String.valueOf(id)));
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.totalHits.value() > 0) {
                return searcher.storedFields().document(topDocs.scoreDocs[0].doc);
            }
        } finally {
            // Always release the searcher when done.
            searcherManager.release(searcher);
        }
        return null;
    }

    /**
     * Adds a new dynamic tag field (TagDefinition) to the index manager.
     * If a tag definition is not added to the index manager, the corresponding tags cannot be indexed or searched.
     * <p>
     * This method should be called when a new TagDefinition is added to the project.
     */
    public synchronized void addTagValueType(String tagName, ValueType vt) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot add tag ValueTypes if pojo is a non-taggable object! %s does not implement Taggable!", pojoMapper.getPojoName()));

        String fieldName = Taggable.makeTagFieldName(tagName);

        synchronized (configLock) {
            PointsConfig pc = getPointsConfigForValueType(vt);
            if (pc != null)
                pointsConfigMap.put(fieldName, pc);

            // For non-text tag types, use KeywordAnalyzer.
            if (vt != ValueType.TEXT)
                fieldAnalyzers.put(fieldName, new KeywordAnalyzer());
            else
                fieldAnalyzers.put(fieldName, SIRIUS_TEXT_ANALYZER);
        }
    }

    /**
     * Remove the configuration for a dynamic tag field.
     */
    @SneakyThrows
    public synchronized void removeTagValueType(String tagName) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot remove tags from non Taggable Object! %s does not implement Taggable!", pojoMapper.getPojoName()));

        String fieldName = Taggable.makeTagFieldName(tagName);
        Analyzer analyzer;
        synchronized (configLock) {
            pointsConfigMap.remove(fieldName);
            analyzer = fieldAnalyzers.remove(fieldName);
        }
        // Only close per-field analyzers we own. TEXT tag fields reuse the shared base analyzer
        // (SIRIUS_TEXT_ANALYZER); closing it would break all further indexing and search.
        if (analyzer != null && analyzer != baseAnalyzer)
            analyzer.close();

        // Search for live docs carrying the tag and update only the modified ones.
        // NOTE: iterating reader.maxDoc() directly would also read logically-deleted documents and
        // resurrect them via updateDocuments; searching only ever returns live documents.
        List<T> modifiedPojos = search(new MatchAllDocsQuery(), Pageable.unpaged())
                .stream()
                .filter(pojo -> {
                    Map<String, Tag> tags = ((Taggable) pojo).getTags();
                    return tags != null && tags.remove(tagName) != null;
                })
                .toList();

        updateDocuments(modifiedPojos);
    }

    public void close() throws IOException {
        // Close all resources even if one fails (IOUtils rethrows the first error with the rest suppressed),
        // and close the SearcherManager before the writer it wraps.
        IOUtils.close(searcherManager, writer, directory);
    }

}
