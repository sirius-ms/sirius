package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.TaggableLuceneDocumentProvider;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.lucene.LuceneUtils;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueFormatter;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class LuceneSearchService implements SearchService {
    private static final Logger log = LoggerFactory.getLogger(LuceneSearchService.class);

    @NotNull
    private final Map<String, ProjectSearchContext> projectSearchContexts = new HashMap<>();
    @NotNull
    protected final ReadWriteLock projectSpaceLock = new ReentrantReadWriteLock();
    @NotNull
    private final Path luceneIndexHome;
    @NotNull
    private final Reader searcher = new Reader(projectSpaceLock, projectSearchContexts);
    @NotNull
    private final Writer indexer = new Writer(projectSpaceLock, projectSearchContexts);


    @NotNull
    AtomicBoolean closed = new AtomicBoolean(false);

    public LuceneSearchService(@NotNull Path luceneIndexHome) {
        this.luceneIndexHome = luceneIndexHome;
    }


    private static Sort convertSort(@Nullable org.springframework.data.domain.Sort springsort) {
        if (springsort == null || springsort.isUnsorted() || springsort.isEmpty())
            return Sort.RELEVANCE;
        throw new UnsupportedOperationException("Sorting not yet implemented.");
    }

    @Override
    public void openOrCreateProjectIndex(@NotNull Project<?> project) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            if (getSearchIndexReader().indexSearchers.containsKey(project.getProjectId()) || getSearchIndexWriter().indexWriters.containsKey(project.getProjectId()))
                throw new IllegalArgumentException("Project already exists in index: " + project.getProjectId());

            projectSearchContexts.put(project.getProjectId(), new ProjectSearchContext(project));

            Path indexDir = luceneIndexHome.resolve(project.getProjectId());
            Files.createDirectories(indexDir);

            Directory directory = new ByteBuffersDirectory();
            getSearchIndexWriter().indexWriters.put(project.getProjectId(), directory);
            getSearchIndexReader().indexSearchers.put(project.getProjectId(), directory);

        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public void closeProjectIndex(@NotNull String projectId, boolean deleteIndexFromDisk) throws IOException {
        projectSpaceLock.writeLock().lock();
        try {
            getSearchIndexWriter().close(projectId);
            getSearchIndexReader().close(projectId);

            if (deleteIndexFromDisk)
                FileUtils.deleteRecursively(luceneIndexHome.resolve(projectId));
        } finally {
            projectSpaceLock.writeLock().unlock();
        }
    }

    @Override
    public Stream<ValueType> getTagValueType(String projectId) {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).tagDefinitions.values().stream();
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    public ValueType getTagValueType(String projectId, String tagName) {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).getTagValueType(tagName);
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }


    @Override
    public void addTagValueType(String projectId, String tagName, ValueType valueType) {
        projectSpaceLock.readLock().lock();
        try {
            projectSearchContexts.get(projectId).addTagValueType(tagName, valueType);
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public boolean removeTagValueType(String projectId, String tagName) {
        projectSpaceLock.readLock().lock();
        try {
            return projectSearchContexts.get(projectId).removeTagValueType(tagName) != null;
        } finally {
            projectSpaceLock.readLock().unlock();
        }
    }

    @Override
    public Reader getSearchIndexReader() {
        return searcher;
    }

    @Override
    public Writer getSearchIndexWriter() {
        return indexer;
    }

    public void checkOpen() {
        if (closed.get())
            throw new IllegalStateException("Index has been closed.");
    }

    @Override
    public void close() throws IOException {
        try {
            getSearchIndexReader().closeAll();
            getSearchIndexWriter().closeAll();
        } finally {
            closed.set(true);
        }
    }

    public static class Reader implements SearchIndexReader {

        @NotNull
        private final Map<String, ProjectSearchContext> searchContexts;
        @NotNull
        private final Map<String, Directory> indexSearchers = new HashMap<>();
        @NotNull
        private final ReadWriteLock projectSpaceLock;

        public Reader(@NotNull ReadWriteLock projectSpaceLock, @NotNull Map<String, ProjectSearchContext> searchContexts) {
            this.searchContexts = searchContexts;
            this.projectSpaceLock = projectSpaceLock;
        }

        @SneakyThrows
        @Override
        public <T> Page<String> search(String projectId, String query, Pageable paging, Class<T> beanClass, @NotNull String idField, @NotNull String defaultField) {
            if (!indexSearchers.containsKey(projectId))
                throw new IllegalArgumentException("No Index found for project " + projectId);

            try (IndexReader reader = DirectoryReader.open(indexSearchers.get(projectId))) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query queryObject = searchContexts.get(projectId).getParser().parse(query, defaultField);
                TopFieldDocs res = searcher.search(queryObject, (int) (paging.getOffset() + paging.getPageSize()), convertSort(paging.getSort()));
                List<String> ids = Arrays.stream(res.scoreDocs).skip(paging.getOffset()).limit(paging.getPageSize())
                        .map(scoreDoc -> {
                            try {
                                return reader.storedFields().document(scoreDoc.doc, Set.of(idField));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).map(d -> d.get(idField)).toList();
                return new PageImpl<>(ids, paging, res.totalHits.value());
            }
        }

        private void close(@NotNull String projectId) {
            projectSpaceLock.writeLock().lock();
            try {
                Directory d = indexSearchers.remove(projectId);
                if (d != null) {
                    try {
                        d.close();
                    } catch (IOException e) {
                        log.error("Error closing index writer. Memory might be leaked. Index might still be locked.", e);
                    }
                }
            } finally {
                projectSpaceLock.writeLock().unlock();
            }
        }

        private void closeAll() {
            projectSpaceLock.writeLock().lock();
            try {
                indexSearchers.keySet().forEach(this::close);
            } finally {
                projectSpaceLock.writeLock().unlock();
            }
        }
    }

    public static class Writer implements SearchIndexWriter {
        @NotNull
        private final Map<String, ProjectSearchContext> searchContexts;
        @NotNull
        private final Map<String, Directory> indexWriters = new HashMap<>();
        @NotNull
        private final ReadWriteLock projectSpaceLock;

        public Writer(@NotNull ReadWriteLock projectSpaceLock, @NotNull Map<String, ProjectSearchContext> searchContexts) {
            this.searchContexts = searchContexts;
            this.projectSpaceLock = projectSpaceLock;
        }


        @SneakyThrows
        @Override
        public <T extends TaggableLuceneDocumentProvider> void addBean(String projectId, T bean) {
            projectSpaceLock.readLock().lock();
            try {
                addBean(projectId, bean.toLuceneDocument(searchContexts.get(projectId)));
            } finally {
                projectSpaceLock.readLock().unlock();
            }
        }

        @SneakyThrows
        @Override
        public <T extends Iterable<IndexableField>> void addBean(String projectId, T bean) {
            projectSpaceLock.readLock().lock();
            try {
                ProjectSearchContext searchContext = searchContexts.get(projectId);
                searchContext.lock.writeLock().lock();
                try (IndexWriter w = new IndexWriter(indexWriters.get(projectId), new IndexWriterConfig(searchContext.analyzer))) {
                    searchContext.addFields(bean);
                    w.addDocument(bean);
                } finally {
                    searchContext.lock.writeLock().unlock();
                }
            } finally {
                projectSpaceLock.readLock().unlock();
            }
        }

        @SneakyThrows
        @Override
        public <T extends TaggableLuceneDocumentProvider> void addBeans(String projectId, Collection<T> beans) {
            projectSpaceLock.readLock().lock();
            try {
                ProjectSearchContext searchContext = searchContexts.get(projectId);
                addBeans(projectId, beans.stream().map(b -> b.toLuceneDocument(searchContext)).toList());
            } finally {
                projectSpaceLock.readLock().unlock();
            }

        }

        @SneakyThrows
        @Override
        public <T extends Iterable<IndexableField>> void addBeans(String projectId, Iterable<T> beans) {
            projectSpaceLock.readLock().lock();
            try {
                ProjectSearchContext searchContext = searchContexts.get(projectId);
                searchContext.lock.writeLock().lock();
                try (IndexWriter w = new IndexWriter(indexWriters.get(projectId), new IndexWriterConfig(searchContext.analyzer))) {
                    beans.forEach(searchContext::addFields);
                    w.addDocuments(beans);
                } finally {
                    searchContext.lock.writeLock().unlock();
                }
            } finally {
                projectSpaceLock.readLock().unlock();
            }
        }

        private void close(@NotNull String projectId) {
            projectSpaceLock.writeLock().lock();
            try {
                Directory dir = indexWriters.remove(projectId);
                if (dir != null) {
                    try {
                        dir.close();
                    } catch (IOException e) {
                        log.error("Error closing index writer. Memory might be leaked. Index might still be locked.", e);
                    }
                }
            } finally {
                projectSpaceLock.writeLock().unlock();
            }
        }

        private void closeAll() {
            projectSpaceLock.writeLock().lock();
            try {
                indexWriters.keySet().forEach(this::close);
            } finally {
                projectSpaceLock.writeLock().unlock();
            }
        }
    }


    public static class ProjectSearchContext {
        private final Map<String, ValueType> tagDefinitions;

        @NotNull
        private final Map<String, Analyzer> fieldAnalyzers = new ConcurrentHashMap<>();
        @NotNull
        private final Analyzer analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), fieldAnalyzers);
        @Getter
        private final StandardQueryParser parser;

        protected final ReadWriteLock lock = new ReentrantReadWriteLock();

        private ProjectSearchContext(@NotNull Project<?> project) {
            this(project.findTags());
        }

        private ProjectSearchContext(Iterable<TagDefinition> tagDefinitionsStr) {
            this.tagDefinitions = new HashMap<>();
            tagDefinitionsStr.forEach(tagDef -> tagDefinitions.put(tagDef.getTagName(), tagDef.getValueType()));
            parser = new StandardQueryParser(analyzer); //todo do we want to have default fields?
            parser.setPointsConfigMap(new HashMap<>());
//            parser = LuceneUtils.makeDefaultQueryParser(tagDefinitionsStr);
        }

        public void addTagValueType(TagDefinition tagDefinition) {
            addTagValueType(tagDefinition.getTagName(), tagDefinition.getValueType());
        }

        public void addTagValueType(String tagName, ValueType valueType) {
            lock.writeLock().lock();
            try {
                tagDefinitions.put(tagName, valueType);
            } finally {
                lock.writeLock().unlock();
            }

        }

        public ValueType removeTagValueType(String tagName) {
            lock.writeLock().lock();
            try {
                return tagDefinitions.remove(tagName);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void addFields(Iterable<IndexableField> fields) {
            if (fields != null) {
                lock.writeLock().lock();
                try {
                    extractPerFieldAnalyzer(fields);
                    extractPointValues(fields);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        public ValueType getTagValueType(String tagName) {
            lock.readLock().lock();
            try {
                return tagDefinitions.get(tagName);
            } finally {
                lock.readLock().unlock();
            }
        }


        private void extractPerFieldAnalyzer(@NotNull Iterable<IndexableField> fields) {
            for (IndexableField f : fields) {
                if (!fieldAnalyzers.containsKey(f.name())) { //this and putIfAbsent is a bit like double-checked locking
                    if (f instanceof StringField || f instanceof KeywordField)
                        fieldAnalyzers.putIfAbsent(f.name(), new KeywordAnalyzer());
                }
            }
        }

        private void extractPointValues(Iterable<IndexableField> fields) {
            Map<String, PointsConfig> pointsConfigMap = parser.getPointsConfigMap();
            for (IndexableField f : fields) {
                if (!pointsConfigMap.containsKey(f.name())) { //this and putIfAbsent is a bit like double-checked locking
                    switch (f) {
                        case FloatField d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Float.class));
                        case FloatPoint d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Float.class));
                        case DoubleField d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Double.class));
                        case DoublePoint d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Double.class));
                        case IntField d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Integer.class));
                        case IntPoint d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Integer.class));
                        case LongField d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Long.class));
                        case LongPoint d ->
                                pointsConfigMap.putIfAbsent(d.name(), new PointsConfig(DecimalFormat.getInstance(), Long.class));
                        default -> {
                            //handle everything else
                        }
                    }
                }
            }
        }


        public IndexableField getIndexableTagField(String tagName, Object formattedTagValue, Field.Store storeValue) {
            lock.readLock().lock();
            try {
                ValueType valueType = getTagValueType(tagName);
                ValueFormatter<?, ?> formatter = valueType.getFormatter();
                String fieldName = LuceneUtils.TAG_FIELD_PREFIX + tagName;
                Object value = formatter.fromFormattedGeneric(formattedTagValue);

                //todo add none or remove it in general.
                return switch (valueType) {
                    case BOOLEAN ->
                            new StringField(fieldName, String.valueOf(formatter.fromFormattedGeneric(formattedTagValue)), storeValue);
                    case INTEGER, TIME ->
                            new IntPoint(fieldName, (Integer) formatter.fromFormattedGeneric(formattedTagValue));
                    case REAL -> new DoublePoint(fieldName, (Double) formatter.fromFormattedGeneric(formattedTagValue));
                    case TEXT -> new TextField(fieldName, (String) value, storeValue);
                    case DATE -> new LongPoint(fieldName, (Long) value);
                    default -> throw new IllegalStateException("Unexpected value: " + valueType);
                };
            } finally {
                lock.readLock().unlock();
            }
        }

    }

}
