package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

public class LuceneSearchService implements SearchService {
    private static final Logger log = LoggerFactory.getLogger(LuceneSearchService.class);

    @NotNull
    private final Analyzer analyzer = new StandardAnalyzer();
    @NotNull
    private final Path luceneIndexHome;
    @NotNull
    private final Reader searcher = new Reader(analyzer);
    @NotNull
    private final Writer indexer = new Writer(analyzer);


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
        getSearchIndexReader().indexSearchers.put(project.getProjectId(),directory);
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

        public Reader(Analyzer analyzer) {
            queryParser = new StandardQueryParser(analyzer); //todo do we want to have default fields?
        }

        @SneakyThrows
        @Override
        public <T> Page<String> search(String projectId, String query, Pageable paging, Class<T> beanClass, @NotNull String idField, @NotNull String defaultField) {
            if (!indexSearchers.containsKey(projectId))
                throw new IllegalArgumentException("No Index found for project " + projectId);

            try (IndexReader reader =  DirectoryReader.open(indexSearchers.get(projectId))) {
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

        public Writer(Analyzer analyzer) {
            this.analyzer = analyzer;
        }

        @SneakyThrows
        @Override
        public <T> void addBean(String projectId, T bean) {
            try (IndexWriter w = new IndexWriter(indexWriters.get(projectId), new IndexWriterConfig(analyzer))) {
                w.addDocument((Iterable<? extends IndexableField>) bean);
            }
        }

        @SneakyThrows
        @Override
        public <T> void addBeans(String projectId, Iterable<T> bean) {
            try (IndexWriter w = new IndexWriter(indexWriters.get(projectId),  new IndexWriterConfig(analyzer))) {
                w.addDocuments(StreamSupport.stream(bean.spliterator(), false).map(b -> (Iterable<? extends IndexableField>) b).toList());
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


}
