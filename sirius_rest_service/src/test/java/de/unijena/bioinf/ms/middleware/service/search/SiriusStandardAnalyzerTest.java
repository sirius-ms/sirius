package de.unijena.bioinf.ms.middleware.service.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.BytesRef; // Import added
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SiriusStandardAnalyzerTest {

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new SiriusStandardAnalyzer();
    }

    @After
    public void tearDown() {
        analyzer.close();
    }

    @Test
    public void testUnderscoreSplitting() throws IOException {
        // Input: "hello_world"
        // Expected with PRESERVE_ORIGINAL:
        // 1. "hello_world" (The original)
        // 2. "hello" (Split part)
        // 3. "world" (Split part)
        List<String> tokens = analyzeText("hello_world");

        assertContainsToken(tokens, "hello_world"); // Raw preserved
        assertContainsToken(tokens, "hello");       // Split part
        assertContainsToken(tokens, "world");       // Split part
    }

    @Test
    public void testDotSplitting() throws IOException {
        List<String> tokens = analyzeText("test.example");

        assertContainsToken(tokens, "test.example");
        assertContainsToken(tokens, "test");
        assertContainsToken(tokens, "example");
    }

    @Test
    public void testHyphenSplitting() throws IOException {
        List<String> tokens = analyzeText("some-hyphen");

        assertContainsToken(tokens, "some-hyphen");
        assertContainsToken(tokens, "some");
        assertContainsToken(tokens, "hyphen");
    }

    @Test
    public void testWhitespaceSplitting() throws IOException {
        // Whitespace tokenizer splits these BEFORE the filter sees them.
        List<String> tokens = analyzeText("hello world");

        assertContainsToken(tokens, "hello");
        assertContainsToken(tokens, "world");
    }

    @Test
    public void testMixedDelimiters() throws IOException {
        List<String> tokens = analyzeText("hello_world test.example some-hyphen");

        // Check for Originals
        assertContainsToken(tokens, "hello_world");
        assertContainsToken(tokens, "test.example");
        assertContainsToken(tokens, "some-hyphen");

        // Check for Parts
        assertContainsToken(tokens, "hello");
        assertContainsToken(tokens, "world");
        assertContainsToken(tokens, "test");
        assertContainsToken(tokens, "example");
        assertContainsToken(tokens, "some");
        assertContainsToken(tokens, "hyphen");
    }

    @Test
    public void testCamelCaseSplitting() throws IOException {
        // Input: "camelCase"
        List<String> tokens = analyzeText("camelCase");

        assertContainsToken(tokens, "camelcase"); // Lowercased Original
        assertContainsToken(tokens, "camel");
        assertContainsToken(tokens, "case");
    }

    @Test
    public void testNumericSplitting() throws IOException {
        // Input: "version2.1"
        List<String> tokens = analyzeText("version2.1");
        //todo this is suboptimal number tokenization, i guess.
        assertContainsToken(tokens, "version2.1"); // Original
        assertContainsToken(tokens, "version2");
        assertContainsToken(tokens, "1");
    }

    @Test
    public void testComplexExample() throws IOException {
        // Input: "Apache_Lucene-4.7.2 is_THE.best"
        List<String> tokens = analyzeText("Apache_Lucene-4.7.2 is_THE.best");

        // Verify Originals
        assertContainsToken(tokens, "apache_lucene-4.7.2");
        assertContainsToken(tokens, "is_the.best");

        // Verify Parts
        assertContainsToken(tokens, "apache");
        assertContainsToken(tokens, "lucene");
        assertContainsToken(tokens, "4");
        assertContainsToken(tokens, "7");
        assertContainsToken(tokens, "2");
        assertContainsToken(tokens, "is");
        assertContainsToken(tokens, "the");
        assertContainsToken(tokens, "best");
    }

    /**
     * This test verifies the fix for the QueryParser Wildcard issue.
     * * The Analyzer.normalize(String, String) method returns a BytesRef
     * and internally throws an IllegalStateException if the normalization
     * produces more than 1 token.
     */
    @Test
    public void testNormalization() {
        // Simulate what the QueryParser does when it sees "G79_BH4*"
        // It calls analyzer.normalize("field", "G79_BH4")

        // This will Throw IllegalStateException if your analyzer splits the token!
        BytesRef normalized = analyzer.normalize("field", "G79_BH4");

        // It should be lowercased
        assertEquals("g79_bh4", normalized.utf8ToString());
    }

    @Test
    public void testThreadSafety() {
        final boolean[] success = {true};
        final Exception[] threadException = {null};

        Runnable task = () -> {
            try {
                List<String> tokens = analyzeText("thread_safety.test-123");

                boolean hasOriginal = tokens.contains("thread_safety.test-123");
                boolean hasPart = tokens.contains("thread");

                if (!hasOriginal || !hasPart) {
                    success[0] = false;
                    System.out.println("Thread failed. Tokens: " + tokens);
                }
            } catch (Exception e) {
                success[0] = false;
                threadException[0] = e;
                System.out.println("Thread exception: " + e.getMessage());
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!success[0]) {
            if (threadException[0] != null) {
                fail("Thread test failed with exception: " + threadException[0].getMessage());
            } else {
                fail("Thread test failed: missing expected tokens");
            }
        }
    }

    // --- Helpers ---

    private void assertContainsToken(List<String> tokens, String expected) {
        assertTrue("Expected tokens to contain '" + expected + "' but found: " + tokens,
                tokens.contains(expected));
    }

    private List<String> analyzeText(String text) throws IOException {
        return analyzeText(text, "field");
    }

    private List<String> analyzeText(String text, String fieldName) throws IOException {
        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(text));
        List<String> tokens = getTokens(stream);
        return tokens;
    }

    private List<String> getTokens(TokenStream stream) throws IOException {
        List<String> tokens = new ArrayList<>();
        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);

        try {
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(termAttr.toString());
            }
        } finally {
            stream.close();
        }

        return tokens;
    }
}