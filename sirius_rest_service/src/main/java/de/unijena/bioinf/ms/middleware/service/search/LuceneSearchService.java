package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
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

public class LuceneSearchService implements SearchService {
    private static final Logger log = LoggerFactory.getLogger(LuceneSearchService.class);

    @NotNull
    private final Map<String, PointsConfig> pointsConfigMap = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, Analyzer> fieldAnalyzers = new ConcurrentHashMap<>();
    @NotNull
    private final Analyzer analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), fieldAnalyzers);
    @NotNull
    private final Path luceneIndexHome;
    @NotNull
    private final Reader searcher = new Reader(analyzer, pointsConfigMap);
    @NotNull
    private final Writer indexer = new Writer(analyzer, fieldAnalyzers, pointsConfigMap);


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
    public synchronized void openOrCreateProjectIndex(@NotNull Project<?> project) throws IOException {
        if (getSearchIndexReader().indexSearchers.containsKey(project.getProjectId()) || getSearchIndexWriter().indexWriters.containsKey(project.getProjectId()))
            throw new IllegalArgumentException("Project already exists in index: " + project.getProjectId());

        Path indexDir = luceneIndexHome.resolve(project.getProjectId());
        Files.createDirectories(indexDir);

//        Directory directory = FSDirectory.open(indexDir);
        Directory directory = new ByteBuffersDirectory();
        getSearchIndexWriter().indexWriters.put(project.getProjectId(), directory);
        getSearchIndexReader().indexSearchers.put(project.getProjectId(), directory);
    }

    @Override
    public synchronized void closeProjectIndex(@NotNull String projectId, boolean deleteIndexFromDisk) throws IOException {
        getSearchIndexWriter().close(projectId);
        getSearchIndexReader().close(projectId);

        if (deleteIndexFromDisk)
            FileUtils.deleteRecursively(luceneIndexHome.resolve(projectId));
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

    public class Reader implements SearchIndexReader {

        @NotNull
        private final StandardQueryParser queryParser; //= new QueryParser(null, new StandardAnalyzer());
        private ConcurrentHashMap<String, Directory> indexSearchers = new ConcurrentHashMap<>();

        public Reader(@NotNull Analyzer analyzer, @NotNull Map<String, PointsConfig> pointsConfigMap) {
            queryParser = new StandardQueryParser(analyzer); //todo do we want to have default fields?
            queryParser.setPointsConfigMap(pointsConfigMap);
        }

        @SneakyThrows
        @Override
        public <T> Page<String> search(String projectId, String query, Pageable paging, Class<T> beanClass, @NotNull String idField, @NotNull String defaultField) {
            if (!indexSearchers.containsKey(projectId))
                throw new IllegalArgumentException("No Index found for project " + projectId);

            try (IndexReader reader = DirectoryReader.open(indexSearchers.get(projectId))) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query queryObject = queryParser.parse(query, defaultField);
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

        private synchronized void close(@NotNull String projectId) {
            Directory d = indexSearchers.remove(projectId);
            if (d != null) {
                try {
                    d.close();
                } catch (IOException e) {
                    log.error("Error closing index writer. Memory might be leaked. Index might still be locked.", e);
                }
            }
        }

        private synchronized void closeAll() {
            indexSearchers.keySet().forEach(this::close);
        }
    }

    //todo multi thread safety
    //todo error handling and type safety
    public static class Writer implements SearchIndexWriter {
        @NotNull
        private final ConcurrentHashMap<String, Directory> indexWriters = new ConcurrentHashMap<>();
        private final Analyzer analyzer;
        private final Map<String, Analyzer> fieldAnalyzers;
        private final Map<String, PointsConfig> pointsConfigMap;

        public Writer(@NotNull Analyzer analyzer, @NotNull Map<String, Analyzer> fieldAnalyzers, @NotNull Map<String, PointsConfig> pointsConfigMap) {
            this.analyzer = analyzer;
            this.fieldAnalyzers = fieldAnalyzers;
            this.pointsConfigMap = pointsConfigMap;
        }

        @SneakyThrows
        @Override
        public <T extends Iterable<IndexableField>> void addBean(String projectId, T bean) {
            try (IndexWriter w = new IndexWriter(indexWriters.get(projectId), new IndexWriterConfig(analyzer))) {
                extractPointValues(bean, pointsConfigMap);
                extractPerFieldAnalyzer(bean, fieldAnalyzers);
                w.addDocument(bean);
            }
        }

        @SneakyThrows
        @Override
        public <T extends Iterable<IndexableField>> void addBeans(String projectId, Iterable<T> beans) {
            try (IndexWriter w = new IndexWriter(indexWriters.get(projectId), new IndexWriterConfig(analyzer))) {
                beans.forEach(b -> extractPerFieldAnalyzer(b, fieldAnalyzers));
                beans.forEach(b -> extractPointValues(b, pointsConfigMap));
                w.addDocuments(beans);
            }
        }

        private synchronized void close(@NotNull String projectId) {
            Directory dir = indexWriters.remove(projectId);
            if (dir != null) {
                try {
                    dir.close();
                } catch (IOException e) {
                    log.error("Error closing index writer. Memory might be leaked. Index might still be locked.", e);
                }
            }
        }

        private synchronized void closeAll() {
            indexWriters.keySet().forEach(this::close);
        }
    }

    public static void extractPerFieldAnalyzer(Iterable<IndexableField> fields, @NotNull final Map<String, Analyzer> fieldAnalyzers) {
        if (fields != null) {
            for (IndexableField f : fields) {
                if (!fieldAnalyzers.containsKey(f.name())) { //this and putIfAbsent is a bit like double-checked locking
                    if (f instanceof StringField || f instanceof KeywordField)
                        fieldAnalyzers.putIfAbsent(f.name(), new KeywordAnalyzer());
                }
            }
        }
    }

    public static void extractPointValues(Iterable<IndexableField> fields, @NotNull final Map<String, PointsConfig> pointsConfigMap) {
        if (fields != null) {
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
    }
}
