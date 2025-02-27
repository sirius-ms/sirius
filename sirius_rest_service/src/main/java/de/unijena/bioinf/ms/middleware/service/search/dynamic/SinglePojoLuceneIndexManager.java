package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.service.lucene.LuceneUtils;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueFormatter;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.projectspace.IndexField;
import it.unimi.dsi.fastutil.Pair;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.NumberDateFormat;
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
import org.springframework.data.domain.Sort;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.util.NumericUtils.doubleToSortableLong;
import static org.apache.lucene.util.NumericUtils.floatToSortableInt;

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

    private final Class<T> pojoClass;
    private final String pojoIdField;
    private final boolean nonStoredFields;

    // Base analyzer and dynamic (per-field) analyzer:
    private final Analyzer baseAnalyzer = new StandardAnalyzer();
    private final Map<String, Analyzer> fieldAnalyzers = new PrefixAwareString2ObjectHashMap<>();
    private final Map<String, PointsConfig> pointsConfigMap = new PrefixAwareString2ObjectHashMap<>();
    private final Map<String, SortField.Type> sortTypes = new PrefixAwareString2ObjectHashMap<>();
    private final PerFieldAnalyzerWrapper dynamicAnalyzer = new PerFieldAnalyzerWrapper(baseAnalyzer, fieldAnalyzers);

    // StandardQueryParser using the dynamic analyzer.
    private final List<CharSequence> defaultSearchFields = new ArrayList<>();
    private final StandardQueryParser queryParser = new StandardQueryParser(dynamicAnalyzer);
    private final ProjectSearchContext projectContext;

    public SinglePojoLuceneIndexManager(ProjectSearchContext projectContext, Map<String, ValueType> initialValueTypes, Class<T> pojoClass) {
        try {
            this.projectContext = projectContext;
            this.pojoClass = pojoClass;

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
            detectAnalyzersAndPointConfigs();
            // Initialize the query with  points config map and default search fields.
            queryParser.setMultiFields(defaultSearchFields.toArray(CharSequence[]::new));
            queryParser.setPointsConfigMap(pointsConfigMap);

            { // detect pojo id field, check for non-stored fields
                String pojoIdFieldTmp = null;
                boolean unStoredTmp = false;

                for (Field f : pojoClass.getDeclaredFields()) {
                    if (f.isAnnotationPresent(IndexField.class)) {
                        IndexField indexField = f.getAnnotation(IndexField.class);
                        String fieldName = indexField.name().isEmpty() ? f.getName() : indexField.name();

                        if (!indexField.stored() && !indexField.documentId())
                            unStoredTmp = true;

                        if (indexField.documentId()) {
                            if (pojoIdFieldTmp != null)
                                throw new IllegalStateException("Document ID field already set. Only one ID field is allowed!");
                            pojoIdFieldTmp = fieldName;
                        }
                    }
                }
                nonStoredFields = unStoredTmp;
                pojoIdField = pojoIdFieldTmp;

                if (pojoIdField == null)
                    throw new IllegalArgumentException("No document ID field defined! ID field is mandatory!");
            }


        } catch (IOException e) {
            throw new RuntimeException("IO Error when initializing Lucene index for " + pojoClass.getSimpleName(), e);
        }
    }

    public boolean isEmpty() {
        return getNumOfDocs().key() <= 0;
    }

    public boolean isTaggable() {
        return Taggable.class.isAssignableFrom(pojoClass);
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
        Document doc = convertPojoToDocument(pojo);
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
                .map(this::convertPojoToDocument)
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
        Document doc = convertPojoToDocument(pojo);
        writer.updateDocument(new Term(pojoIdField, doc.get(pojoIdField)), doc);
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
                .map(this::convertPojoToDocument)
                .toList();

        Query q = new TermInSetQuery(pojoIdField, docs.stream()
                .map(doc -> doc.get(pojoIdField))
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
        deleteDocumentById(getPojoId(pojoToRemove));
    }

    public synchronized void deleteDocuments(@NotNull Collection<T> pojosToRemove) {
        deleteDocumentsById(pojosToRemove.stream().map(this::getPojoId).toList());
    }

    @SneakyThrows
    public synchronized void deleteDocumentById(@NotNull Object id) {
        writer.deleteDocuments(new Term(pojoIdField, String.valueOf(id)));
        writer.commit();
        getNumOfDocs();
    }

    @SneakyThrows
    public synchronized void deleteDocumentsById(@NotNull Collection<Object> ids) {
        if (ids.isEmpty())
            return;
        Query q = new TermInSetQuery(pojoIdField, ids.stream().map(Object::toString).map(BytesRef::new).collect(Collectors.toSet()));
        writer.deleteDocuments(q);
        writer.commit();
        getNumOfDocs();
    }

    public synchronized void updateDocumentsFields(Collection<Object> docIds, Consumer<T> modifier) throws IllegalArgumentException {
        Query q = new TermInSetQuery(pojoIdField, docIds.stream()
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
        if (hasNonStoredFields()){
            String msg = String.format(NON_STORED_FIELDS_MESSAGE, pojoClass.getSimpleName(), docId);
            log.warn(msg);
            throw new UnsupportedOperationException(msg);
        }

        Document doc = searchDocumentById(docId);
        if (doc == null)
            return Optional.empty();

        T pojo = convertDocumentToPojo(doc, pojoClass);
        modifier.accept(pojo);
        updateDocument(pojo);
        return Optional.of(pojo);
    }

    public synchronized void addTagsToDocuments(Collection<Object> docIds, Collection<Tag> tags) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot add tags to non Taggable Object! %s does not implement Taggable!", pojoClass.getSimpleName()));

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
            throw new UnsupportedOperationException(String.format("Cannot add tags to non Taggable Object! %s does not implement Taggable!", pojoClass.getSimpleName()));


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
            throw new UnsupportedOperationException(String.format("Cannot remove tags from non Taggable Object! %s does not implement Taggable!", pojoClass.getSimpleName()));

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
        return searchAndTransform(query, pageable, doc -> convertDocumentToPojo(doc, pojoClass));
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
        return searchAndTransform(query, pageable, doc -> doc.get(pojoIdField));
    }

    @SneakyThrows
    private synchronized <R> Page<R> searchAndTransform(Query query, Pageable pageable, Function<Document, R> function) {
        searcherManager.maybeRefresh();
        IndexSearcher searcher = searcherManager.acquire();
        try {
            int numDocs = searcher.getIndexReader().numDocs();
            if (numDocs <= 0)
                return Page.empty(pageable);

            org.apache.lucene.search.Sort sort = convertToLuceneSort(pageable);
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
            Query query = new TermQuery(new Term(pojoIdField, String.valueOf(id)));
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
            throw new UnsupportedOperationException(String.format("Cannot remove tags from non Taggable Object! %s does not implement Taggable!", pojoClass.getSimpleName()));

        String fieldName = LuceneUtils.TAG_FIELD_PREFIX + tagName;

        PointsConfig pc = getPointsConfigForValueType(vt);
        if (pc != null)
            pointsConfigMap.put(fieldName, pc);

        // For non-text tag types, use KeywordAnalyzer.
        if (vt != ValueType.TEXT)
            fieldAnalyzers.put(fieldName, new KeywordAnalyzer());
        else
            fieldAnalyzers.put(fieldName, new StandardAnalyzer()); //todo do we need to configure full text search as well or doe default work?
    }

    /**
     * Remove the configuration for a dynamic tag field.
     */
    @SneakyThrows
    public synchronized void removeTagValueType(String tagName) {
        if (!isTaggable())
            throw new UnsupportedOperationException(String.format("Cannot remove tags from non Taggable Object! %s does not implement Taggable!", pojoClass.getSimpleName()));

        String fieldName = LuceneUtils.TAG_FIELD_PREFIX + tagName;
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
                T pojo = convertDocumentToPojo(doc, pojoClass);
                if (((Taggable) pojo).getTags().remove(tagName) != null)
                    modifiedPojos.add(pojo);
            }
        } finally {
            // Always release the searcher when done.
            searcherManager.release(searcher);
        }

        updateDocuments(modifiedPojos);
    }

    @SneakyThrows
    private Object getPojoId(T pojo) {
        Field f = pojo.getClass().getField(pojoIdField);
        f.setAccessible(true);
        return f.get(pojo);
    }

    /**
     * Returns true if any field annotated with @IndexField in the bean class is not stored.
     */
    public boolean hasNonStoredFields() {
        return nonStoredFields;
    }

    /**
     * Converts a bean into a Lucene Document.
     * <p>
     * It iterates over all fields annotated with @IndexField and adds one or more Lucene fields
     * (using an appropriate field type for numbers, text, etc.). For collections/arrays and nested objects,
     * the helper method createAnnotationFields handles the conversion.
     * Finally, if the bean implements Taggable, dynamic tag fields are added.
     */
    private Document convertPojoToDocument(T pojo) {
        Document doc = new Document();
        for (Field field : pojo.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(IndexField.class)) {
                field.setAccessible(true);
                IndexField indexField = field.getAnnotation(IndexField.class);
                String fieldName = indexField.name().isEmpty() ? field.getName() : indexField.name();
                try {
                    Object value = field.get(pojo);
                    if (value == null)
                        continue;
                    List<IndexableField> luceneFields = createAnnotationFields(fieldName, value, indexField.stored() || indexField.documentId(), indexField.fullTextSearch(), indexField.sortable());
                    luceneFields.forEach(doc::add);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        // If the bean is taggable, add dynamic tag fields.
        if (isTaggable() && pojo instanceof Taggable taggablePojo) {
            Map<String, Tag> tags = taggablePojo.getTags();
            if (tags != null) {
                for (Tag tag : tags.values()) {
                    ValueType vt = projectContext.getTagValueType(tag.getTagName());
                    List<IndexableField> tagFields = createTagFields(tag.getTagName(), tag.getValue(), vt);
                    tagFields.forEach(doc::add);
                }
            }
        }
        return doc;
    }

    /**
     * Converts a Lucene Document back into a bean.
     * In addition to converting simple annotated fields, this method also supports:
     * <ul>
     *   <li>Collection/array–typed fields (by calling doc.getValues(fieldName)).</li>
     *   <li>Nested objects (by scanning for fields with names prefixed with the nested field’s name).</li>
     *   <li>Enum conversion (using Enum.valueOf).</li>
     * </ul>
     * Finally, dynamic tag fields (fields starting with the tag prefix) are collected into the bean’s tag map.
     */
    private T convertDocumentToPojo(Document doc, Class<T> clazz) {
        return convertDocumentToPojo("", doc, clazz);
    }

    private <C> C convertDocumentToPojo(String fieldPrefix, Document doc, Class<C> clazz) {
        try {
            C instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(IndexField.class)) {
                    field.setAccessible(true);
                    IndexField ann = field.getAnnotation(IndexField.class);
                    String fieldName = fieldPrefix + (ann.name().isEmpty() ? field.getName() : ann.name());
                    Class<?> fieldType = field.getType();

                    if (isMap(fieldType)) { //handle hash maps
                        String prefix = fieldName + ".";
                        IndexableField[] values = doc.getFields().stream().filter(storedField -> storedField.name().startsWith(prefix)).toArray(IndexableField[]::new);
                        if (values.length > 0) {
                            Map<String, Object> map = new HashMap<>();
                            for (IndexableField storedField : values) {
                                String key = storedField.name().substring(prefix.length());
                                Class<?> valueType = getMapValueType(field);
                                Object value = convertStoredValue(storedField, valueType);
                                map.put(key, value);
                            }
                            field.set(instance, map);
                        }
                    } else if (isCollection(fieldType)) { // Handle collections or arrays.
                        IndexableField[] values = doc.getFields(fieldName);
                        if (values != null && values.length > 0) {
                            Class<?> elementType = getCollectionElementType(field);
                            if (fieldType.isArray()) {
                                Object array = Array.newInstance(elementType, values.length);
                                for (int i = 0; i < values.length; i++)
                                    Array.set(array, i, convertStoredValue(values[i], elementType));

                                field.set(instance, array);
                            } else {
                                Collection<Object> convertedList = newCollection((Class<Collection<Object>>) fieldType);
                                for (IndexableField v : values)
                                    convertedList.add(convertStoredValue(v, elementType));
                                field.set(instance, convertedList);
                            }
                        } else {
                            field.set(instance, null);
                        }
                    } else if (isSimpleType(fieldType)) { // Handle simple types.
                        IndexableField storedValue = doc.getField(fieldName);
                        if (storedValue != null) {
                            Object converted = convertStoredValue(storedValue, fieldType);
                            field.set(instance, converted);
                        }
                    } else { // Otherwise, assume nested object.
                        Object nestedInstance = convertDocumentToPojo(fieldName + ".", doc, fieldType);
                        field.set(instance, nestedInstance);
                    }
                }
            }
            // todo replace with generic Map<String,?> type!?
            // Process dynamic tag fields.
            if (instance instanceof Taggable taggablePojo) {
                Map<String, Tag> tagsMap = new HashMap<>();
                for (IndexableField storedField : doc.getFields()) {
                    String name = storedField.name();
                    if (name.startsWith(LuceneUtils.TAG_FIELD_PREFIX)) {
                        // Extract tag name (i.e. remove the prefix "tags.")
                        Tag tag = convertToTag(doc.getField(name));
                        if (tag != null)
                            tagsMap.put(tag.getTagName(), tag);
                    }
                }
                taggablePojo.setTags(tagsMap);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void detectAnalyzersAndPointConfigs() {
        detectAnalyzersAndPointConfigs("", pojoClass, pointsConfigMap, fieldAnalyzers, defaultSearchFields, sortTypes);
    }

    private org.apache.lucene.search.Sort convertToLuceneSort(@NotNull Pageable pageable) {
        return convertToLuceneSort(pageable, sortTypes);
    }

    public void close() throws IOException {
        writer.close();
        searcherManager.close();
        directory.close();
    }



    /**
     * Helper: returns a PointsConfig for a given ValueType.
     */
    public static PointsConfig getPointsConfigForValueType(ValueType valueType) {
        return switch (valueType) {
            case INTEGER -> new PointsConfig(DecimalFormat.getInstance(), Integer.class);

            case REAL -> new PointsConfig(DecimalFormat.getInstance(), Double.class);

            case DATE -> new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class);

            case TIME -> new PointsConfig(new NumberDateFormat(new SimpleDateFormat("HH:mm:ss")), Integer.class);

            default -> null;
        };
    }


    /**
     * Converts the Sort information from a Spring Data Pageable to a Lucene Sort.
     *
     * @param pageable the Spring Data Pageable that contains sort instructions
     * @return a Lucene Sort object representing the same sort orders, or null if no sort is defined
     */
    public static org.apache.lucene.search.Sort convertToLuceneSort(@NotNull Pageable pageable, @NotNull Map<String, SortField.Type> fieldNameToSortType) {
        Sort springSort = pageable.getSort();
        if (springSort.isUnsorted())
            return null; // No sort specified

        List<SortField> sortFields = new ArrayList<>();
        for (Sort.Order order : springSort) {
            // Determine the Lucene field type.
            SortField.Type fieldType = fieldNameToSortType.get(order.getProperty());
            if (fieldType != null)
                sortFields.add(new SortField(order.getProperty(), fieldType, order.isDescending()));
            else
                log.warn("Sort field {} is not supported or at least not registered as sortable field. Ignoring!", order.getProperty());
        }

        if (sortFields.isEmpty())
            return null;

        return new org.apache.lucene.search.Sort(sortFields.toArray(SortField[]::new));
    }


    public static void detectAnalyzersAndPointConfigs(
            @NotNull final String fieldPrefix,
            @NotNull final Class<?> pojoClass,
            @NotNull final Map<String, PointsConfig> pointsConfigMap,
            @NotNull final Map<String, Analyzer> analyzerMap,
            @NotNull final List<CharSequence> defaultSearchFields,
            @NotNull final Map<String, SortField.Type> sortTypes
    ) {
        for (Field field : pojoClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(IndexField.class)) {
                field.setAccessible(true);
                IndexField indexField = field.getAnnotation(IndexField.class);
                String fieldName = fieldPrefix + (indexField.name().isEmpty() ? field.getName() : indexField.name());
                // Handle get element type and take care about collections/arrays.
                Class<?> elementType = field.getType();
                if (isCollection(elementType))
                    elementType = getCollectionElementType(field);
                else if (isMap(elementType)){
                    elementType = getMapValueType(field);
                    if (!isSimpleType(elementType))
                        throw new IllegalArgumentException("Only simple types are allowed as map values.");
                    fieldName = fieldName + ".*";
                }

                if (!isSimpleType(elementType)) {
                    detectAnalyzersAndPointConfigs(fieldName + ".", elementType, pointsConfigMap, analyzerMap, defaultSearchFields, sortTypes);
                } else {
                    PointsConfig pointsConfig = getPointsConfigForType(elementType);
                    if (pointsConfig != null)
                        pointsConfigMap.put(fieldName, pointsConfig);
                    else if (indexField.fullTextSearch() && (elementType.equals(String.class) || elementType.isEnum()))
                        analyzerMap.put(fieldName, new StandardAnalyzer());
                    else // this covers the boolean values as well
                        analyzerMap.put(fieldName, new KeywordAnalyzer());

                    if (indexField.defaultSearchField())
                        defaultSearchFields.add(fieldName);

                    if (indexField.sortable()) {
                        SortField.Type sortType = getSortTypeForType(elementType);
                        if (sortType != null)
                            sortTypes.put(fieldName, sortType);
                    }
                }
            }
        }
    }


    /**
     * Creates one or more Lucene fields for a given bean field value.
     * This method has been extended to support:
     * <ul>
     *   <li>Collections/arrays: each element is processed individually.</li>
     *   <li>Enums: converted to their name.</li>
     *   <li>Nested objects: its own @IndexField–annotated fields are indexed using a qualified name.</li>
     * </ul>
     */
    private static List<IndexableField> createAnnotationFields(@NotNull String fieldName, @Nullable Object value, boolean store, boolean fulltext, boolean sorted) {
        return createAnnotationFields(fieldName, value, store, fulltext, sorted, false);
    }

    private static List<IndexableField> createAnnotationFields(@NotNull String fieldName, @Nullable Object value, boolean store, boolean fulltext, boolean sorted, boolean inCollection) {
        List<IndexableField> fields = new ArrayList<>();
        org.apache.lucene.document.Field.Store storeOption = store ? YES : NO;
        if (value == null)
            return fields;

        //todo do we need to skip this due to have faster parsing time?
        if (!isValidFieldName(fieldName))
            throw new IllegalArgumentException("Invalid field name: " + fieldName);

        // Handle maps. This implementation assumes that the key of the map can be transformed into a valid field name string.
        if (value instanceof Map<?, ?> map) {
            if (!map.isEmpty()) {
                if (map.keySet().iterator().next() instanceof String) {
                    map.forEach((k, v) ->
                            fields.addAll(createAnnotationFields(fieldName + "." + k, v, store, fulltext, sorted, true)));
                } else {
                    throw new IllegalArgumentException("Automatic indexing of hashmaps is only possible with string keys: " + fieldName);
                }
            }
            return fields;
        }

        // Handle collections.
        if (value instanceof Collection<?> coll) {
            for (Object element : coll) {
                fields.addAll(createAnnotationFields(fieldName, element, store, fulltext, sorted, true));
            }
            return fields;
        }
        // Handle arrays.
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                fields.addAll(createAnnotationFields(fieldName, Array.get(value, i), store, fulltext, sorted, true));
            }
            return fields;
        }
        // Handle enums.
        if (value.getClass().isEnum()) {
            String enumVal = ((Enum<?>) value).name();
            fields.add(new StringField(fieldName, enumVal, storeOption));
            if (sorted)
                fields.add(new SortedDocValuesField(fieldName, new BytesRef(enumVal)));
            return fields;
        }
        // Handle nested objects (if not a simple type).
        if (!isSimpleType(value.getClass())) {
            boolean hasNestedIndexFields = false;
            for (Field nested : value.getClass().getDeclaredFields()) {
                if (nested.isAnnotationPresent(IndexField.class)) {
                    hasNestedIndexFields = true;
                    break;
                }
            }
            if (hasNestedIndexFields) {
                for (Field nested : value.getClass().getDeclaredFields()) {
                    if (nested.isAnnotationPresent(IndexField.class)) {
                        nested.setAccessible(true);
                        IndexField nestedAnn = nested.getAnnotation(IndexField.class);
                        String nestedFieldName = nestedAnn.name().isEmpty() ? nested.getName() : nestedAnn.name();
                        String combinedName = fieldName + "." + nestedFieldName;
                        Object nestedValue;
                        try {
                            nestedValue = nested.get(value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        if (nestedValue != null) {
                            fields.addAll(createAnnotationFields(combinedName, nestedValue, nestedAnn.stored() || nestedAnn.documentId(), nestedAnn.fullTextSearch(), nestedAnn.stored(), inCollection));
                        }
                    }
                }
                return fields;
            }
        }
        // Otherwise, treat as a simple type.
        switch (value) {
            case Integer n -> {
                fields.add(new IntPoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, n) : new NumericDocValuesField(fieldName, n));
            }
            case Long n -> {
                fields.add(new LongPoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, n) : new NumericDocValuesField(fieldName, n));
            }
            case Double n -> {
                fields.add(new DoublePoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, doubleToSortableLong(n)) : new DoubleDocValuesField(fieldName, n));
            }
            case Float n -> {
                fields.add(new FloatPoint(fieldName, n));
                if (store)
                    fields.add(new StoredField(fieldName, n));
                if (sorted)
                    fields.add(inCollection ? new SortedNumericDocValuesField(fieldName, floatToSortableInt(n)) : new FloatDocValuesField(fieldName, n));
            }
            case Boolean b -> {
                String s = String.valueOf(b);
                fields.add(new StringField(fieldName, s, storeOption));
                if (sorted)
                    fields.add(new SortedDocValuesField(fieldName, new BytesRef(s)));
            }
            default -> {
                // Fallback: use the object's toString() (also covers String!).
                String s = value.toString();
                if (fulltext)
                    fields.add(new TextField(fieldName, s, storeOption));
                else
                    fields.add(new StringField(fieldName, s, storeOption));
                if (sorted)
                    fields.add(new SortedDocValuesField(fieldName, new BytesRef(s)));
            }
        }
        return fields;
    }


    private Tag convertToTag(IndexableField tagField) {
        String tagName = tagField.name().substring(LuceneUtils.TAG_FIELD_PREFIX.length());
        @NotNull ValueType valueType = projectContext.getTagValueType(tagName);
        Object formattedValue = null;

        switch (valueType) {
            case BOOLEAN -> formattedValue = Boolean.valueOf(tagField.stringValue());
            case INTEGER -> formattedValue = Integer.valueOf(tagField.stringValue());
            case REAL -> formattedValue = tagField.numericValue().doubleValue();
            case TEXT, DATE, TIME -> formattedValue = tagField.stringValue();
        }

        return Tag.builder().tagName(tagName).value(formattedValue).build();
    }


    /**
     * Creates Lucene fields for a dynamic tag.
     * Tags are always stored.
     */
    private static List<IndexableField> createTagFields(String tagName, Object formattedValue, ValueType valueType) {
        final String fieldName = LuceneUtils.TAG_FIELD_PREFIX + tagName;
        List<IndexableField> fields = new ArrayList<>();
        ValueFormatter<?, ?> formatter = valueType.getFormatter();
        Object value = formatter.fromFormattedGeneric(formattedValue);
        // always stored
        switch (valueType) {
            case BOOLEAN -> fields.add(new StringField(fieldName, value.toString(), YES));
            case INTEGER -> {
                fields.add(new IntPoint(fieldName, (Integer) value));
                fields.add(new StoredField(fieldName, (Integer) value));
            }
            case TIME -> {
                fields.add(new IntPoint(fieldName, (Integer) value));
                fields.add(new StoredField(fieldName, (String) formattedValue));
            }
            case REAL -> {
                fields.add(new DoublePoint(fieldName, (Double) value));
                fields.add(new StoredField(fieldName, (Double) value));

            }
            case TEXT -> fields.add(new TextField(fieldName, (String) value, YES));
            case DATE -> {
                fields.add(new LongPoint(fieldName, (Long) value));
                fields.add(new StoredField(fieldName, (String) formattedValue));
            }
            default -> throw new IllegalArgumentException("Unsupported ValueType for tag: " + valueType);
        }
        return fields;
    }

    /**
     * Determines whether a type is considered “simple” (primitive, wrapper, String, or enum).
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.equals(String.class)
                || Number.class.isAssignableFrom(type)
                || type.equals(Boolean.class)
                || type.isEnum();
    }

    /**
     * Returns true if the type is a Collection or an Array.
     */
    private static boolean isMap(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the type is a Collection or an Array.
     */
    private static boolean isCollection(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || type.isArray();
    }


    /**
     * Returns the key type for a map-typed field.
     * If the field is not parameterized, String is returned as fallback.
     */
    private static Class<?> getMapKeyType(Field field) {
        return getCollectionElementType(field);
    }

    /**
     * Returns the value type for a map-typed field.
     * If the field is not parameterized, String is returned as fallback.
     */
    private static Class<?> getMapValueType(Field field) {
        if (!Map.class.isAssignableFrom(field.getType()))
            throw new IllegalArgumentException("Field is not a Map type: " + field.getName());

        try {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return (Class<?>) pt.getActualTypeArguments()[1]; // Value type
        } catch (Exception e) {
            return String.class;
        }
    }


    /**
     * Returns the element type for a collection- or array-typed field.
     * If the field is not parameterized, String is returned as fallback.
     */
    private static Class<?> getCollectionElementType(Field field) {
        if (field.getType().isArray()) {
            return field.getType().getComponentType();
        } else {
            try {
                ParameterizedType pt = (ParameterizedType) field.getGenericType();
                return (Class<?>) pt.getActualTypeArguments()[0];
            } catch (Exception e) {
                return String.class;
            }
        }
    }

    /**
     * Converts a stored string value into an instance of the given type.
     * Supports numeric types, booleans, and enums.
     */
    private static Object convertStoredValue(IndexableField value, Class<?> type) {
        if (type.equals(String.class))
            return value.stringValue();
        else if (type.equals(Integer.class) || type.equals(int.class))
            return value.numericValue().intValue();
        else if (type.equals(Long.class) || type.equals(long.class))
            return value.numericValue().longValue();
        else if (type.equals(Double.class) || type.equals(double.class))
            return value.numericValue().doubleValue();
        else if (type.equals(Float.class) || type.equals(float.class))
            return value.numericValue().floatValue();
        else if (type.equals(Boolean.class) || type.equals(boolean.class))
            return Boolean.parseBoolean(value.stringValue());
        else if (type.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumType = (Class<? extends Enum>) type;
            return Enum.valueOf(enumType, value.stringValue());
        }
        return value;
    }

    /**
     * Creates an instance of a collection based on the given fieldType.
     * If fieldType is an interface, a default implementation is returned:
     * - List       -> ArrayList
     * - Set        -> HashSet
     * - SortedSet  -> TreeSet
     * - Queue      -> LinkedList
     * <p>
     * If fieldType is a concrete class, an instance is created using its no-argument constructor.
     *
     * @param fieldType the Class of the Collection to instantiate
     * @param <T>       the type of the Collection
     * @return an instance of the requested collection type
     * @throws IllegalArgumentException if the type cannot be instantiated or is unsupported
     */
    @SuppressWarnings("unchecked")
    public static <T extends Collection<?>> T newCollection(Class<T> fieldType) {
        if (fieldType.isInterface()) {
            // Return default implementations for well-known collection interfaces.
            if (List.class.isAssignableFrom(fieldType)) {
                return (T) new ArrayList<>();
            } else if (SortedSet.class.isAssignableFrom(fieldType)) {
                return (T) new TreeSet<>();
            } else if (Set.class.isAssignableFrom(fieldType)) {
                return (T) new HashSet<>();
            } else if (Queue.class.isAssignableFrom(fieldType)) {
                return (T) new LinkedList<>();
            } else {
                throw new IllegalArgumentException("Unsupported collection interface: " + fieldType.getName());
            }
        } else {
            // Try to create an instance of a concrete collection class.
            try {
                return fieldType.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new IllegalArgumentException("Could not instantiate collection of type: " + fieldType.getName(), e);
            }
        }
    }

    /**
     * Returns a PointsConfig if the type is numeric (or a date/time type that can be represented numerically).
     * If the type is not numeric, returns null.
     */
    @Nullable
    public static PointsConfig getPointsConfigForType(Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return new PointsConfig(DecimalFormat.getInstance(), Integer.class);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return new PointsConfig(DecimalFormat.getInstance(), Long.class);
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return new PointsConfig(DecimalFormat.getInstance(), Double.class);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return new PointsConfig(DecimalFormat.getInstance(), Float.class);
        } else if (type.equals(java.util.Date.class)) {  // Optionally, if we want to support dates as normal fields:
            return new PointsConfig(new NumberDateFormat(new SimpleDateFormat("yyyy-MM-dd")), Long.class);
        }
        return null;
    }

    /**
     * Returns a SortField.Type if depending on the type.
     * If the type is not supported for sorting, returns null.
     */
    @Nullable
    public static SortField.Type getSortTypeForType(Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return SortField.Type.INT;
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return SortField.Type.LONG;
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return SortField.Type.DOUBLE;
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return SortField.Type.FLOAT;
        } else if (type.equals(boolean.class) || type.equals(Boolean.class) || type.equals(String.class)) {
            return SortField.Type.STRING;
        }
        return null;
    }


    // Regular expression for a valid Lucene field name with sub-documents
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$");

    /**
     * Checks if the given field name is valid for a Lucene document.
     *
     * @param fieldName The field name to validate.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        Matcher matcher = FIELD_NAME_PATTERN.matcher(fieldName);
        return matcher.matches();
    }

}
