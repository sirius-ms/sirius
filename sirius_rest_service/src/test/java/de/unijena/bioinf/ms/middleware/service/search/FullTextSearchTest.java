package de.unijena.bioinf.ms.middleware.service.search;

import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.*;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.middleware.model.tags.TagDefinition;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.InchiKey2DQueryRewriter;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.LuceneDirectoryPersistenceUtils;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.ms.persistence.model.core.PersistentSearchIndex;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.projectspace.IndexField;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FullTextSearchTest {

    // Test POJO with a full-text searchable field
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPojo {
        @IndexField(name = "id", documentId = true)
        public String id;

        @IndexField(name = "normalField")
        public String normalField;

        @IndexField(name = "fulltextField", fullTextSearch = true)
        public String fulltextField;

        @IndexField(name = "defaultSearchField", fullTextSearch = true, defaultSearchField = true)
        public String defaultSearchField;

    }

    private SearchService searchService;
    private Project<?> mockProject;
    private String projectId = "test-project";

    @BeforeEach
    public void setup() throws IOException {
        // Create a mock project
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getProjectId()).thenReturn(projectId);
        Mockito.when(mockProject.getSystemUID()).thenReturn("test-system-uid");
        Mockito.when(mockProject.findTags()).thenReturn(Collections.emptyList());

        // Create a search service with in-memory indices
        searchService = new SearchServiceImpl(project -> {
            Map<String, ValueType> tagDefinitions = new HashMap<>();
            for (Object item : project.findTags()) {
                TagDefinition td = (TagDefinition) item;
                tagDefinitions.put(td.getTagName(), td.getValueType());
            }
            return new PerPojoSearchContext(null, tagDefinitions);
        });
        searchService.openOrCreateProjectIndex(mockProject);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(mockProject, true);
    }

    @Test
    public void testBasicFullTextSearch() {
        // Add a test document with a value containing "BH4"
        TestPojo pojo = new TestPojo(
            "1", 
            "normal value",
            "G79624_5x_BH4_01_18895",
            "Some default searchable text BH4"
        );
        
        searchService.addDocument(projectId, pojo);
        
        // Search for "BH4" in all fields
        Page<TestPojo> results = searchService.search(
            projectId, 
            "BH4",
            PageRequest.of(0, 10), 
            TestPojo.class
        );
        
        // We expect to find our document
        assertEquals(1, results.getTotalElements(), 
            "Should find one document when searching for 'BH4'");
    }
    
    @Test
    public void testExplicitFieldSearch() {
        // Add a test document with a value containing "BH4"
        TestPojo pojo = new TestPojo(
            "1", 
            "normal value",
            "G79624_5x_BH4_01_18895",
            "Some default searchable text"
        );
        
        searchService.addDocument(projectId, pojo);
        
        // Search with explicit field name
        Page<TestPojo> results = searchService.search(
            projectId, 
            "fulltextField:BH4", 
            PageRequest.of(0, 10), 
            TestPojo.class
        );
        
        assertEquals(1, results.getTotalElements(), 
            "Should find one document when explicitly searching fulltextField for 'BH4'");
    }
    
    @Test
    public void testTokenizationWithSiriusStandardAnalyzer() throws IOException {
        // Test how StandardAnalyzer tokenizes our text
        Analyzer analyzer = new SiriusStandardAnalyzer();
        String text = "G79624_5x_BH4_01_18895";
        
        try (TokenStream stream = analyzer.tokenStream("field", text)) {
            CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            
            boolean foundBH4Token = false;
            System.out.println("Tokens for: " + text);
            
            while (stream.incrementToken()) {
                String token = termAttr.toString();
                System.out.println(" - " + token);
                if (token.equals("bh4")) { // StandardAnalyzer lowercases tokens
                    foundBH4Token = true;
                }
            }
            
            assertTrue(foundBH4Token, 
                "StandardAnalyzer should produce 'bh4' token from the input text");
        }
    }
    
    @Test
    public void testDirectLuceneSearching() throws IOException, QueryNodeException {
        // Test directly with Lucene APIs to isolate the issue
        Directory directory = new ByteBuffersDirectory();
        Analyzer analyzer = new SiriusStandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            // Add document with our test value
            Document doc = new Document();
            doc.add(new TextField("content", "G79624_5x_BH4_01_18895", Field.Store.YES));
            writer.addDocument(doc);
            writer.commit();
        }
        
        // Search for "BH4"
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            StandardQueryParser queryParser = new StandardQueryParser(analyzer);
            Query query = queryParser.parse("BH4", "content");
            
            TopDocs results = searcher.search(query, 10);
            assertEquals(1, results.totalHits.value(),
                "Direct Lucene search should find the document when searching for 'BH4'");
        }
    }
    
    @Test
    public void testCaseInsensitiveSearch() {
        // Test case insensitivity
        TestPojo pojo = new TestPojo(
            "1", 
            "normal value",
            "G79624_5x_BH4_01_18895", 
            "Some default searchable text with BH4"
        );
        
        searchService.addDocument(projectId, pojo);
        
        // Search for lowercase version
        Page<TestPojo> results = searchService.search(
            projectId, 
            "bh4", 
            PageRequest.of(0, 10), 
            TestPojo.class
        );
        
        assertEquals(1, results.getTotalElements(), 
            "Case-insensitive search should find document when searching for lowercase 'bh4'");
    }
    
    @Test
    public void testWildcardSearch() {
        // Test wildcard search
        TestPojo pojo = new TestPojo(
            "1", 
            "normal value",
            "G79624_5x_BH4_01_18895", 
            "Some default searchable text"
        );
        
        searchService.addDocument(projectId, pojo);
        
        // Search with wildcards
        Page<TestPojo> results = searchService.search(
            projectId, 
            "fulltextField:BH*",
            PageRequest.of(0, 10), 
            TestPojo.class
        );
        
        assertEquals(1, results.getTotalElements(), 
            "Wildcard search should find the document when searching for 'BH*'");
    }
    
    @Test
    public void testDefaultSearchField() {
        // Test that defaultSearchField works correctly
        TestPojo pojo = new TestPojo(
            "1", 
            "normal value",
            "not in default search field", 
            "This text contains BH4 term"
        );
        
        searchService.addDocument(projectId, pojo);
        
        // Search without specifying field (should use default search fields)
        Page<TestPojo> results = searchService.search(
            projectId, 
            "BH4", 
            PageRequest.of(0, 10), 
            TestPojo.class
        );
        
        assertEquals(1, results.getTotalElements(), 
            "Should find document when searching default fields for 'BH4'");
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class NestedPojo {
        @IndexField(name = "nestedField")
        public String nestedField;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentPojo {
        @IndexField(name = "id", documentId = true)
        public String id;

        @IndexField(name = "nested")
        public NestedPojo nested;
    }

    @Test
    public void testNoUnqualifiedSuffixMatchingForNestedFields() {
        ParentPojo parent = new ParentPojo("parent-1", new NestedPojo("nested-value"));
        searchService.addDocument(projectId, parent);

        // 1. Fully qualified query should succeed
        Page<ParentPojo> fqResults = searchService.search(
            projectId, 
            "nested.nestedField:nested-value", 
            PageRequest.of(0, 10), 
            ParentPojo.class
        );
        assertEquals(1, fqResults.getTotalElements(), 
            "Fully qualified nested field query should find the document");

        // 2. Unqualified query (just suffix) should fail (0 hits) now that unqualified matching is removed
        Page<ParentPojo> uqResults = searchService.search(
            projectId, 
            "nestedField:nested-value", 
            PageRequest.of(0, 10), 
            ParentPojo.class
        );
        assertEquals(0, uqResults.getTotalElements(), 
            "Unqualified nested field query should NOT find the document since unqualified matching has been removed");
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class InchiKeyPojo {
        @IndexField(name = "id", documentId = true)
        public String id;

        @IndexField(name = "inchiKey", queryRewriter = InchiKey2DQueryRewriter.class)
        public String inchiKey;
    }

    @Test
    public void testInchiKeyQueryRewriterAndOptimization() {
        InchiKeyPojo pojo = new InchiKeyPojo("inchikey-1", "WZPVREJFMGASTU");
        searchService.addDocument(projectId, pojo);

        // 1. Fully qualified, full-length (27 chars) InChIKey search should get rewritten to 14 chars and find the document
        Page<InchiKeyPojo> fullResults = searchService.search(
            projectId, 
            "inchiKey:WZPVREJFMGASTU-UHFFFAOYSA-N", 
            PageRequest.of(0, 10), 
            InchiKeyPojo.class
        );
        assertEquals(1, fullResults.getTotalElements(), 
            "Full-length InChIKey query should be rewritten to 14 chars and successfully find the document");

        // 2. Already 14-char InChIKey search should not need rewriting (retaining query object reference via optimization) and succeed
        Page<InchiKeyPojo> shortResults = searchService.search(
            projectId, 
            "inchiKey:WZPVREJFMGASTU", 
            PageRequest.of(0, 10), 
            InchiKeyPojo.class
        );
        assertEquals(1, shortResults.getTotalElements(), 
            "Already truncated 14-char InChIKey query should not require rewriting and successfully find the document");
    }

    @Test
    public void testLuceneDirectoryPersistenceUtilsSerialization() throws IOException, QueryNodeException {
        // 1. Create a directory and add some test document
        Directory sourceDir = new ByteBuffersDirectory();
        Analyzer analyzer = new SiriusStandardAnalyzer();
        try (IndexWriter writer = new IndexWriter(sourceDir, new IndexWriterConfig(analyzer))) {
            Document doc = new Document();
            doc.add(new TextField("content", "persistence test success", Field.Store.YES));
            writer.addDocument(doc);
            writer.commit();
        }

        // 2. Serialize directory directly to in-memory byte array
        byte[] serializedData = LuceneDirectoryPersistenceUtils.serialize(sourceDir);
        assertNotNull(serializedData);
        assertTrue(serializedData.length > 0);

        // 3. Deserialize into a completely new directory
        Directory targetDir = new ByteBuffersDirectory();
        LuceneDirectoryPersistenceUtils.deserialize(serializedData, targetDir);

        // 4. Assert the deserialized directory is identical and searchable
        try (IndexReader reader = DirectoryReader.open(targetDir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            StandardQueryParser parser = new StandardQueryParser(analyzer);
            Query query = parser.parse("persistence", "content");
            TopDocs hits = searcher.search(query, 10);
            assertEquals(1, hits.totalHits.value(), 
                "Search on deserialized directory should successfully find the indexed document");
        }
    }

    @Test
    public void testFullTextSearchIndexDatabaseLifecyclePersistence() throws Exception {
        java.nio.file.Path dbFile = Files.createTempFile("nitrite-lifecycle-test", ".db");
        try {
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();
            
            // Warm-up: open and close once to permanently create all collections and indices on disk first!
            try (NitriteDatabase warmupDb = new NitriteDatabase(dbFile, metadata)) {
                warmupDb.flush();
            }

            NitriteDatabase db = new NitriteDatabase(dbFile, metadata);

            // Mock the project and space manager to return our database
            NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
            NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
            SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

            Mockito.when(project.getProjectId()).thenReturn("lifecycle-project");
            Mockito.when(project.getSystemUID()).thenReturn("lifecycle-system-uid");
            Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
            Mockito.when(project.isTempProject()).thenReturn(false);
            Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
            Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
            Mockito.when(mockProjDb.getStorage()).thenReturn(db);

            // 1. Start the search service and add a document
            SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider1 =
                proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
            SearchService service1 = new SearchServiceImpl(provider1);
            service1.openOrCreateProjectIndex(project);

            TestPojo pojo = new TestPojo("lifecycle-pojo-1", "normal-val", "full-val", "This matches lifecycle-bh4");
            service1.addDocument("lifecycle-project", pojo);

            // Verify it is there
            Page<TestPojo> initialResults = service1.search("lifecycle-project", "lifecycle-bh4", Pageable.unpaged(), TestPojo.class);
            assertEquals(1, initialResults.getTotalElements());

            // 2. Close the search service. This should trigger serialization and upsert to db!
            service1.close();

            db.close(); // Close database to commit changes and increment MVStore version

            // Re-open database
            NitriteDatabase db2 = new NitriteDatabase(dbFile, metadata);

            Mockito.when(mockProjDb.getStorage()).thenReturn(db2);

            // Verify the index data is written into the Nitrite database
            Optional<PersistentSearchIndex> savedIndex = db2.getByPrimaryKey("TestPojo", PersistentSearchIndex.class);
            assertTrue(savedIndex.isPresent(), "Index must be persisted to the database upon close");
            assertNotNull(savedIndex.get().getIndexData());
            assertTrue(savedIndex.get().getIndexData().length > 0);

            // 3. Start a new search service and open the same project. It should restore from db!
            SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider2 =
                proj -> new PerPojoDatabaseSearchContext<>(db2, null, Collections.emptyMap());
            SearchService service2 = new SearchServiceImpl(provider2);
            service2.openOrCreateProjectIndex(project);

            // Search without rebuilding the index - it must be loaded from DB!
            Page<TestPojo> restoredResults = service2.search("lifecycle-project", "lifecycle-bh4", Pageable.unpaged(), TestPojo.class);
            assertEquals(1, restoredResults.getTotalElements(), "Index should be restored from Nitrite database on startup");

            service2.close();
            db2.close();
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testEmptyIndexDeletion() throws Exception {
        java.nio.file.Path dbFile = Files.createTempFile("nitrite-empty-test", ".db");
        try {
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();
            NitriteDatabase db = new NitriteDatabase(dbFile, metadata);

            NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
            NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
            SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

            Mockito.when(project.getProjectId()).thenReturn("empty-test-project");
            Mockito.when(project.getSystemUID()).thenReturn("empty-test-system-uid");
            Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
            Mockito.when(project.isTempProject()).thenReturn(false);
            Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
            Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
            Mockito.when(mockProjDb.getStorage()).thenReturn(db);

            // 1. Open project index, insert a document, and close (should save to DB)
            SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider1 =
                proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
            SearchService service1 = new SearchServiceImpl(provider1);
            service1.openOrCreateProjectIndex(project);

            TestPojo pojo = new TestPojo("empty-pojo-1", "normal-val", "full-val", "matches bh4-empty-test");
            service1.addDocument("empty-test-project", pojo);
            service1.close();

            // Verify index is saved
            Optional<PersistentSearchIndex> savedIndex1 = db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class);
            assertTrue(savedIndex1.isPresent(), "Index must exist in DB initially");

            // 2. Open project index, remove the document (index becomes empty), and close (should remove from DB)
            SearchService service2 = new SearchServiceImpl(provider1);
            service2.openOrCreateProjectIndex(project);
            service2.removeDocumentById("empty-test-project", "empty-pojo-1", TestPojo.class);
            service2.close();

            // Verify index is deleted from DB
            Optional<PersistentSearchIndex> savedIndex2 = db.getByPrimaryKey("TestPojo", PersistentSearchIndex.class);
            assertFalse(savedIndex2.isPresent(), "Index must be deleted from Nitrite database when empty");

            db.close();
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testCorruptionFallback() throws Exception {
        java.nio.file.Path dbFile = Files.createTempFile("nitrite-corruption-test", ".db");
        try {
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();
            NitriteDatabase db = new NitriteDatabase(dbFile, metadata);

            NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
            NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
            SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

            Mockito.when(project.getProjectId()).thenReturn("corruption-project");
            Mockito.when(project.getSystemUID()).thenReturn("corruption-system-uid");
            Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
            Mockito.when(project.isTempProject()).thenReturn(false);
            Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
            Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
            Mockito.when(mockProjDb.getStorage()).thenReturn(db);

            // Insert corrupted bytes into PersistentSearchIndex
            byte[] corruptedBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
            db.upsert(new PersistentSearchIndex("TestPojo", corruptedBytes));

            // Setup search service with database provider
            SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider =
                proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
            SearchService service = new SearchServiceImpl(provider);

            // This should not crash, it should gracefully fall back to a fresh/empty index!
            assertDoesNotThrow(() -> service.openOrCreateProjectIndex(project),
                "Corruption fallback should handle deserialization failure gracefully and not crash openOrCreateProjectIndex");

            // Verify search works on the fallback index (is empty but functional)
            Page<TestPojo> results = service.search("corruption-project", "bh4", Pageable.unpaged(), TestPojo.class);
            assertEquals(0, results.getTotalElements(), "Fallback index should be empty");

            service.close();
            db.close();
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testIndexStaleStateDetectionAndRebuilt() throws Exception {
        java.nio.file.Path dbFile = Files.createTempFile("nitrite-stale-test", ".db");
        try {
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();

            // Warm-up: open and close once to permanently create all collections and indices on disk first!
            try (NitriteDatabase warmupDb = new NitriteDatabase(dbFile, metadata)) {
                warmupDb.flush();
            }

            // 1. First run: save a valid index
            try (NitriteDatabase db = new NitriteDatabase(dbFile, metadata)) {
                NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
                NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
                SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

                Mockito.when(project.getProjectId()).thenReturn("stale-project");
                Mockito.when(project.getSystemUID()).thenReturn("stale-system-uid");
                Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
                Mockito.when(project.isTempProject()).thenReturn(false);
                Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
                Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
                Mockito.when(mockProjDb.getStorage()).thenReturn(db);

                SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider =
                    proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
                SearchService service = new SearchServiceImpl(provider);
                service.openOrCreateProjectIndex(project);

                TestPojo pojo = new TestPojo("pojo-1", "normal-val", "full-val", "stale-bh4-test");
                service.addDocument("stale-project", pojo);

                service.close(); // Saves index and commits V_close version
            }

            // 2. Simulate legacy software modification (untracked commit)
            try (NitriteDatabase legacyDb = new NitriteDatabase(dbFile, metadata)) {
                // Perform multiple separate writes and flushes to simulate extensive legacy software modifications
                for (int i = 0; i < 10; i++) {
                    legacyDb.upsert(new PersistentSearchIndex("FakeClass" + i, new byte[]{1, 2, 3}));
                    legacyDb.flush(); // Bumps MVStore version
                }
            }

            // 3. Open project index again on new service. It should detect stale index and gracefully reindex/empty it
            try (NitriteDatabase db = new NitriteDatabase(dbFile, metadata)) {
                NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
                NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
                SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

                Mockito.when(project.getProjectId()).thenReturn("stale-project");
                Mockito.when(project.getSystemUID()).thenReturn("stale-system-uid");
                Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
                Mockito.when(project.isTempProject()).thenReturn(false);
                Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
                Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
                Mockito.when(mockProjDb.getStorage()).thenReturn(db);

                SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider =
                    proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
                SearchService service = new SearchServiceImpl(provider);
                service.openOrCreateProjectIndex(project);

                // Since it detected a mismatch, it gracefully cleared/emptied the stale index directory to trigger rebuilding
                Page<TestPojo> results = service.search("stale-project", "stale-bh4-test", Pageable.unpaged(), TestPojo.class);
                assertEquals(0, results.getTotalElements(), "Stale index should have been discarded and cleared");

                service.close();
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }

    @Test
    public void testCompactionIndexPreservation() throws Exception {
        java.nio.file.Path dbFile = Files.createTempFile("nitrite-compact-test", ".db");
        try {
            Metadata metadata = MsProjectDocumentDatabase.buildMetadata();

            // Warm-up: open and close once to permanently create all collections and indices on disk first!
            try (NitriteDatabase warmupDb = new NitriteDatabase(dbFile, metadata)) {
                warmupDb.flush();
            }

            try (NitriteDatabase db = new NitriteDatabase(dbFile, metadata)) {
                NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
                NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
                SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

                Mockito.when(project.getProjectId()).thenReturn("compact-project");
                Mockito.when(project.getSystemUID()).thenReturn("compact-system-uid");
                Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
                Mockito.when(project.isTempProject()).thenReturn(false);
                Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
                Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
                Mockito.when(mockProjDb.getStorage()).thenReturn(db);

                SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider =
                    proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
                SearchService service = new SearchServiceImpl(provider);
                service.openOrCreateProjectIndex(project);

                TestPojo pojo = new TestPojo("pojo-1", "normal-val", "full-val", "compact-bh4-test");
                service.addDocument("compact-project", pojo);

                service.close();
            }

            System.out.println("COMPACT STEP START");

            // Perform compaction (this will rewrite file and re-anchor index versions)
            try (NitriteDatabase db = new NitriteDatabase(dbFile, metadata)) {
                System.out.println("COMPACTING...");
                db.compact();
                System.out.println("COMPACTED.");

                NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
                Mockito.doReturn(db).when(project).storage();

                NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
                Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
                Mockito.when(mockSpaceManager.getLocation()).thenReturn(dbFile.toAbsolutePath().toString());

                SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider =
                    proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
                SearchService service = new SearchServiceImpl(provider);
                System.out.println("RE-ANCHORING...");
                service.reanchorStorageCommitVersion(project, dbFile.toAbsolutePath());
                System.out.println("RE-ANCHORED.");
            }

            System.out.println("COMPACT STEP END");

            // Verify index is still loaded successfully and searchable
            try (NitriteDatabase db = new NitriteDatabase(dbFile, metadata)) {
                NoSQLProjectImpl project = Mockito.mock(NoSQLProjectImpl.class);
                NoSQLProjectSpaceManager mockSpaceManager = Mockito.mock(NoSQLProjectSpaceManager.class);
                SiriusProjectDatabaseImpl mockProjDb = Mockito.mock(SiriusProjectDatabaseImpl.class);

                Mockito.when(project.getProjectId()).thenReturn("compact-project");
                Mockito.when(project.getSystemUID()).thenReturn("compact-system-uid");
                Mockito.when(project.findTags()).thenReturn(Collections.emptyList());
                Mockito.when(project.isTempProject()).thenReturn(false);
                Mockito.when(project.getProjectSpaceManager()).thenReturn(mockSpaceManager);
                Mockito.when(mockSpaceManager.getProject()).thenReturn(mockProjDb);
                Mockito.when(mockProjDb.getStorage()).thenReturn(db);

                SearchContextProvider<NoSQLProjectImpl, PerPojoDatabaseSearchContext<?>> provider =
                    proj -> new PerPojoDatabaseSearchContext<>(db, null, Collections.emptyMap());
                SearchService service = new SearchServiceImpl(provider);
                service.openOrCreateProjectIndex(project);

                // Verify the index was preserved perfectly after compact and we can still search the restored index
                Page<TestPojo> results = service.search("compact-project", "compact-bh4-test", Pageable.unpaged(), TestPojo.class);
                assertEquals(1, results.getTotalElements(), "Index should have been preserved and restored perfectly after compaction");

                service.close();
            }
        } finally {
            Files.deleteIfExists(dbFile);
        }
    }
}