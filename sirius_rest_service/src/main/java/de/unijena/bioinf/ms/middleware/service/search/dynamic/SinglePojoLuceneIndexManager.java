package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.search.mappers.GenericPojoMapper;
import de.unijena.bioinf.ms.middleware.service.search.mappers.TagMapper;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import it.unimi.dsi.fastutil.Pair;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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


    private final GenericPojoMapper<T> pojoMapper;

    // Base analyzer and dynamic (per-field) analyzer:
    private final Analyzer baseAnalyzer = SIRIUS_TEXT_ANALYZER;
    private final Map<String, Analyzer> fieldAnalyzers = new PrefixAwareString2ObjectHashMap<>();
    private final Map<String, PointsConfig> pointsConfigMap = new PrefixAwareString2ObjectHashMap<>();
    private final Map<String, SortField.Type> sortTypes = new PrefixAwareString2ObjectHashMap<>();
    private final PerFieldAnalyzerWrapper dynamicAnalyzer = new PerFieldAnalyzerWrapper(baseAnalyzer, fieldAnalyzers);

    // StandardQueryParser using the dynamic analyzer.
    private final List<CharSequence> defaultSearchFields = new ArrayList<>();
    private final StandardQueryParser queryParser = new StandardQueryParser(dynamicAnalyzer);

    public SinglePojoLuceneIndexManager(ProjectSearchContext projectContext, Map<String, ValueType> initialValueTypes, Class<T> pojoClass) {
        try {
            this.pojoMapper = new GenericPojoMapper<>(pojoClass, new TagMapper(projectContext::getTagValueType));


            //init lucene index.
            if (projectContext.getProjectIndexRootDir() != null) {
                Path indexPath = projectContext.getProjectIndexRootDir().resolve(pojoClass.getSimpleName());
                Files.createDirectories(indexPath);
                this.directory = FSDirectory.open(indexPath);
            } else {
                this.directory = new ByteBuffersDirectory();
                System.out.println("Using in memory index for: " + pojoClass.getSimpleName());
            }

            this.writer = new IndexWriter(directory, new IndexWriterConfig(dynamicAnalyzer));
            this.searcherManager = new SearcherManager(writer, null);

            // Scans add all existing TagDefinitions to set PointConfigs and Analyzers
            initialValueTypes.forEach(this::addTagValueType);

            // Scans the bean class’s @IndexField annotations to set PointConfigs and Analyzers.
            // Adds PointsConfig entries to the query parser.
            // Adds default search fields to query parser
            pojoMapper.detectAnalyzersAndPointConfigs(pointsConfigMap, fieldAnalyzers, defaultSearchFields, sortTypes);
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

    @SneakyThrows
    public Pair<Integer, Integer> getNumOfDocs() {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            int numdocs = searcher.getIndexReader().numDocs();
            int maxdocs = searcher.getIndexReader().maxDoc();

            System.out.println("Number of docs im index: " + numdocs + "/" + maxdocs);
            return Pair.of(numdocs, maxdocs);
        } finally {
            searcherManager.release(searcher);
        }
    }

    // low level lucene document handling
    @SneakyThrows
    public synchronized void addDocument(@NotNull T pojo) {
        Document doc = pojoMapper.toDocument(pojo);
        writer.addDocument(doc);
        writer.commit();
        getNumOfDocs();
//        System.out.println("AFTER ADD DOCUMENT");
//        printDocs();
    }

    @SneakyThrows
    public synchronized void addDocuments(@NotNull Collection<T> pojos) {
        if (pojos.isEmpty())
            return;
        getNumOfDocs();
        StopWatch watch = StopWatch.createStarted();
        List<Document> docs = pojos.stream()
                .map(pojoMapper::toDocument)
                .toList();
        System.out.println("Creating " + docs.size() + " Docs took: " + watch);
        watch.stop();
        watch.reset();
        watch.start();

        writer.addDocuments(docs);
        writer.commit();
        System.out.println("Writing " + docs.size() + " Docs to Index took: " + watch);
        watch.stop();
        watch.reset();
        watch.start();
//        System.out.println("AFTER MULTI ADD DOCUMENT");
//        printDocs();
        getNumOfDocs();
    }

    @SneakyThrows
    public synchronized void updateDocument(@NotNull T pojo) {
        Document doc =  pojoMapper.toDocument(pojo);
        writer.updateDocument(new Term(pojoMapper.getPojoIdField(), doc.get(pojoMapper.getPojoIdField())), doc);
        writer.commit();
//        System.out.println("AFTER UPDATE DOCUMENT");
//        printDocs();
        getNumOfDocs();
    }

    @SneakyThrows
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

        writer.updateDocuments(q, docs);
        writer.commit();
//        System.out.println("AFTER MULTI UPDATE DOCUMENTS");
//        printDocs();
        getNumOfDocs();
    }

    public synchronized void deleteDocument(@NotNull T pojoToRemove) {
        deleteDocumentById(pojoMapper.getIdValue(pojoToRemove));
    }

    public synchronized void deleteDocuments(@NotNull Collection<T> pojosToRemove) {
        deleteDocumentsById(pojosToRemove.stream().map(pojoMapper::getIdValue).toList());
    }

    @SneakyThrows
    public synchronized void deleteDocumentById(@NotNull Object id) {
        writer.deleteDocuments(new Term(pojoMapper.getPojoIdField(), String.valueOf(id)));
        writer.commit();
        getNumOfDocs();
    }

    @SneakyThrows
    public synchronized void deleteDocumentsById(@NotNull Collection<?> ids) {
        if (ids.isEmpty())
            return;
        Query q = new TermInSetQuery(pojoMapper.getPojoIdField(), ids.stream().map(Object::toString).map(BytesRef::new).collect(Collectors.toSet()));
        writer.deleteDocuments(q);
        writer.commit();
        getNumOfDocs();
    }

    public synchronized void updateDocumentsFields(Collection<?> docIds, Consumer<T> modifier) throws IllegalArgumentException {
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
        if (pojoMapper.isNonStoredFields()){
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

    public synchronized void addTagsToDocuments(Collection<?> docIds, Collection<Tag> tags) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot add tags to non Taggable Object! %s does not implement Taggable!", pojoMapper.getPojoName()));

        updateDocumentsFields(docIds, pojo -> {
            Map<String, Tag> tagMap = ((Taggable) pojo).getTags();
            if (tagMap == null)
                tagMap = new HashMap<>();

            for (Tag tag : tags)
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
    public synchronized Page<T> search(@Nullable String query, Pageable pageable) {
        return search(Utils.isNullOrBlank(query) ? new MatchAllDocsQuery() : queryParser.parse(query, null), pageable);
    }

    /**
     * Searches the index using the given query and returns a Spring Data Page of beans.
     */
    @SneakyThrows
    public synchronized Page<T> search(Query query, Pageable pageable) {
        return searchAndTransform(query, pageable, pojoMapper::toPojo);
    }


    @SneakyThrows
    public synchronized Page<String> searchIds(@Nullable String query, Pageable pageable) {
        return searchIds(Utils.isNullOrBlank(query) ? new MatchAllDocsQuery() : queryParser.parse(query, null), pageable);
    }

    /**
     * Searches the index using the given query and returns a Spring Data Page of beans.
     */
    @SneakyThrows
    public synchronized Page<String> searchIds(Query query, Pageable pageable) {
        return searchAndTransform(query, pageable, doc -> doc.get(pojoMapper.getPojoIdField()));
    }

    @SneakyThrows
    private synchronized <R> Page<R> searchAndTransform(Query query, Pageable pageable, Function<Document, R> function) {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            int numDocs = searcher.getIndexReader().numDocs();
            if (numDocs <= 0)
                return Page.empty(pageable);

            org.apache.lucene.search.Sort sort = convertToLuceneSort(pageable, sortTypes);
            int numHits = pageable.isUnpaged() ? numDocs : Math.min(numDocs, (int) (pageable.getOffset() + pageable.getPageSize()));
            //todo add search after mechanism for better deep pagination

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            TopDocs topDocs = sort != null ? searcher.search(query, numHits, sort) : searcher.search(query, numHits);
            System.out.println("LUCENE: REAL SEARCHING with " + numHits + " took: " + stopWatch);
            stopWatch.stop();
            stopWatch.reset();
            stopWatch.start();

            ScoreDoc[] hits = topDocs.scoreDocs;
            int start = pageable.isUnpaged() ? 0 : (int) pageable.getOffset();
            int end = pageable.isUnpaged() ? hits.length : Math.min(start + pageable.getPageSize(), hits.length);
            List<R> results = new ArrayList<>(end - start);
            for (int i = start; i < end; i++)
                results.add(function.apply(searcher.storedFields().document(hits[i].doc)));

            System.out.println("LUCENE: Collecting results took: " + stopWatch);

            return new PageImpl<>(results, pageable, topDocs.totalHits.value());
        } finally {
            // Always release the searcher when done.
            searcherManager.release(searcher);
        }
    }

    @SneakyThrows
    @Nullable
    private synchronized Document searchDocumentById(Object id) {
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

        PointsConfig pc = getPointsConfigForValueType(vt);
        if (pc != null)
            pointsConfigMap.put(fieldName, pc);

        // For non-text tag types, use KeywordAnalyzer.
        if (vt != ValueType.TEXT)
            fieldAnalyzers.put(fieldName, new KeywordAnalyzer());
        else
            fieldAnalyzers.put(fieldName, SIRIUS_TEXT_ANALYZER);
    }

    /**
     * Remove the configuration for a dynamic tag field.
     */
    @SneakyThrows
    public synchronized void removeTagValueType(String tagName) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot remove tags from non Taggable Object! %s does not implement Taggable!", pojoMapper.getPojoName()));

        String fieldName = Taggable.makeTagFieldName(tagName);
        queryParser.getPointsConfigMap().remove(fieldName);
        Analyzer analyzer = fieldAnalyzers.remove(fieldName);
        if (analyzer != null)
            analyzer.close();

        //searching for docs with the tag to be removed.
        //update only the modified ones!
        List<T> modifiedPojos = new ArrayList<>();
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            IndexReader reader = searcher.getIndexReader();
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.storedFields().document(i);
                T pojo = pojoMapper.toPojo(doc);
                if (((Taggable) pojo).getTags().remove(tagName) != null)
                    modifiedPojos.add(pojo);
            }
        } finally {
            // Always release the searcher when done.
            searcherManager.release(searcher);
        }

        updateDocuments(modifiedPojos);
    }

    public void close() throws IOException {
        writer.close();
        searcherManager.close();
        directory.close();
    }

}
