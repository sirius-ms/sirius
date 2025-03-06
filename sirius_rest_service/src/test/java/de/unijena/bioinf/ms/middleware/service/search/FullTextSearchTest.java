package de.unijena.bioinf.ms.middleware.service.search;

import static org.junit.jupiter.api.Assertions.*;

import de.unijena.bioinf.ms.middleware.service.projects.Project;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.PerPojoProjectSearchContext;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.SearchServiceImpl;
import de.unijena.bioinf.projectspace.IndexField;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

import java.io.IOException;
import java.util.Collections;

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

      /*  private Map<String, Tag> tags = new HashMap<>();

        @Override
        public Map<String, Tag> getTags() {
            return tags;
        }

        @Override
        public void setTags(Map<String, Tag> tags) {
            this.tags = tags;
        }*/
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
        searchService = new SearchServiceImpl(null, PerPojoProjectSearchContext.FACTORY);
        searchService.openOrCreateProjectIndex(mockProject);
    }

    @AfterEach
    public void cleanup() throws IOException {
        searchService.closeProjectIndex(projectId, true);
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
    public void testTokenizationWithStandardAnalyzer() throws IOException {
        // Test how StandardAnalyzer tokenizes our text
        Analyzer analyzer = new StandardAnalyzer();
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
        Analyzer analyzer = new StandardAnalyzer();
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
}